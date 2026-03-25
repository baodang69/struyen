package com.struyen.struyen.services;

import com.struyen.struyen.dtos.OtruyenDto;
import com.struyen.struyen.entities.Category;
import com.struyen.struyen.entities.Chapter;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.repositories.CategoryRepository;
import com.struyen.struyen.repositories.ChapterRepository;
import com.struyen.struyen.repositories.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.scheduling.annotation.Async;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtruyenSyncService {

    private final RestTemplate restTemplate;
    private final StoryRepository storyRepository;
    private final CategoryRepository categoryRepository;
    private final ChapterRepository chapterRepository;

    private static final String API_NEW_STORIES = "https://otruyenapi.com/v1/api/danh-sach/truyen-moi?page=";
    private static final String API_STORY_DETAIL = "https://otruyenapi.com/v1/api/truyen-tranh/";

    /**
     * Đồng bộ danh sách truyện mới báo cáo trên API (trang 1)
     */
    public void syncNewStories(int page) {
        log.info("Started syncing new stories at page {}", page);
        String url = API_NEW_STORIES + page;
        ResponseEntity<OtruyenDto.BaseResponse<OtruyenDto.ListData>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<OtruyenDto.BaseResponse<OtruyenDto.ListData>>() {}
        );

        if (response.getBody() != null && "success".equals(response.getBody().getStatus())) {
            List<OtruyenDto.StoryItem> items = response.getBody().getData().getItems();
            
            // 1. Áp dụng Bộ lọc Tiền Tuyến (Pre-filtering)
            List<String> slugs = items.stream().map(OtruyenDto.StoryItem::getSlug).toList();
            List<Story> existingStories = storyRepository.findBySlugIn(slugs);
            Map<String, LocalDateTime> currentDates = existingStories.stream()
                    .filter(s -> s.getOriginUpdatedAt() != null)
                    .collect(java.util.stream.Collectors.toMap(Story::getSlug, Story::getOriginUpdatedAt));

            for (OtruyenDto.StoryItem item : items) {
                try {
                    LocalDateTime apiTime = parseDate(item.getUpdatedAt());
                    LocalDateTime dbTime = currentDates.get(item.getSlug());
                    
                    // CẮT BỎ LUỒNG I/O NẾU DỮ LIỆU ĐÃ MỚI NHẤT
                    if (dbTime != null && apiTime != null && !apiTime.isAfter(dbTime)) {
                        log.info("⚡ Tối ưu Filter: Bỏ qua truyện {} vì đã up-to-date", item.getSlug());
                        continue;
                    }
                    syncStoryDetail(item.getSlug());
                } catch (Exception e) {
                    log.error("Error syncing story slug {}: {}", item.getSlug(), e.getMessage());
                }
            }
        }
        log.info("Finished syncing new stories at page {}", page);
    }

    /**
     * Đồng bộ thông minh (Catch-Up): Chỉ cào các trang có truyện mới hơn thời điểm Sync cuối cùng trong DB.
     */
    public void syncCatchUpStories() {
        LocalDateTime lastSyncTime = storyRepository.findMaxOriginUpdatedAt();
        if (lastSyncTime == null) {
            log.info("Database đang trống (hoặc chưa có truyện), tiến hành cào dữ liệu trang 1...");
            syncNewStories(1);
            return;
        }

        log.info("BẮT ĐẦU CATCH-UP SYNC. Thời điểm truyện mới nhất trong DB: {}", lastSyncTime);
        int page = 1;
        boolean isUpToDate = false;

        while (!isUpToDate) {
            String url = API_NEW_STORIES + page;
            try {
                ResponseEntity<OtruyenDto.BaseResponse<OtruyenDto.ListData>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<OtruyenDto.BaseResponse<OtruyenDto.ListData>>() {}
                );

                if (response.getBody() == null || !"success".equals(response.getBody().getStatus())) {
                    log.error("Lỗi khi load danh sách page {}", page);
                    break;
                }

                List<OtruyenDto.StoryItem> items = response.getBody().getData().getItems();
                if (items == null || items.isEmpty()) {
                    break;
                }

                log.info("Đang quét Catch-Up tại trang {} ...", page);
                
                // Tối ưu bộ lọc CatchUp
                List<String> slugs = items.stream().map(OtruyenDto.StoryItem::getSlug).toList();
                List<Story> existingStories = storyRepository.findBySlugIn(slugs);
                Map<String, LocalDateTime> currentDates = existingStories.stream()
                        .filter(s -> s.getOriginUpdatedAt() != null)
                        .collect(java.util.stream.Collectors.toMap(Story::getSlug, Story::getOriginUpdatedAt));

                for (OtruyenDto.StoryItem chDto : items) {
                    LocalDateTime itemTime = parseDate(chDto.getUpdatedAt());
                    
                    // Nếu thời gian cập nhật của truyện trên API CŨ HƠN mốc truyện mới nhất của chúng ta
                    if (itemTime.isBefore(lastSyncTime)) {
                        log.info("Đã lùi đến mốc thời gian an toàn (truyện cũ mốc {}). Kết thúc Catch-Up Sync tại trang {}.", lastSyncTime, page);
                        isUpToDate = true;
                        break; 
                    }
                    
                    LocalDateTime dbTime = currentDates.get(chDto.getSlug());
                    if (dbTime != null && !itemTime.isAfter(dbTime)) {
                        log.info("⚡ Tối ưu Filter CatchUp: Bỏ qua truyện {} vì đã up-to-date", chDto.getSlug());
                        continue;
                    }
                    
                    // Ngược lại, nếu truyện trên API mới hơn hoặc bằng (do update sát nút), xử lý ngay:
                    syncStoryDetail(chDto.getSlug());
                }
                
                if (!isUpToDate) {
                    page++;
                    // Giới hạn an toàn quét bù tối đa 50 trang
                    if (page > 50) {
                        log.warn("Đã đạt mốc giới hạn quét lùi 50 trang. Ép buộc dừng Catch-Up Sync để bảo vệ tải.");
                        break;
                    }
                    Thread.sleep(1000); 
                }

            } catch (Exception e) {
                log.error("Lỗi khi Catch-Up Sync trang {}: {}", page, e.getMessage());
                break;
            }
        }
        log.info("HOÀN TẤT CATCH-UP SYNC!");
    }

    /**
     * Đồng bộ hàng loạt cho Admin (Chạy ngầm Async để không Timeout API)
     */
    @Async
    public void syncFullStoriesAsync(int startPage, int endPage) {
        log.info("--- BẮT ĐẦU QUÁ TRÌNH CÀO FULL DATA TỪ TRANG {} ĐẾN {} ---", startPage, endPage);
        for (int i = startPage; i <= endPage; i++) {
            try {
                syncNewStories(i);
                log.info("Đã quét xong trang {}. Đang đợi 2 giây để tránh bị chặn IP...", i);
                Thread.sleep(2000); // Ngủ 2 giây để fake tốc độ crawl của con người, lách luật DDoS
            } catch (Exception e) {
                log.error("Lỗi khi cào dữ liệu tại trang {}: {}", i, e.getMessage());
            }
        }
        log.info("--- HOÀN TẤT QUÁ TRÌNH CÀO FULL DATA ---");
    }

    /**
     * Đồng bộ chi tiết một truyển bằng slug. Khởi tạo Category, Story và Chapter.
     */
    @Transactional
    public void syncStoryDetail(String slug) {
        String url = API_STORY_DETAIL + slug;
        ResponseEntity<OtruyenDto.BaseResponse<OtruyenDto.DetailData>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<OtruyenDto.BaseResponse<OtruyenDto.DetailData>>() {}
        );

        if (response.getBody() == null || !"success".equals(response.getBody().getStatus())) {
            return;
        }

        OtruyenDto.StoryItem apiStory = response.getBody().getData().getItem();
        LocalDateTime apiUpdatedTime = parseDate(apiStory.getUpdatedAt());

        Optional<Story> existingOptional = storyRepository.findBySlug(slug);
        Story story;

        if (existingOptional.isPresent()) {
            story = existingOptional.get();
            // Nếu truyện cũ lấy trên DB có ngày cập nhật = API -> bỏ qua không sync lại
            if (story.getOriginUpdatedAt() != null && apiUpdatedTime != null && 
                !apiUpdatedTime.isAfter(story.getOriginUpdatedAt())) {
                log.info("Story {} is already up to date. Skipping.", slug);
                return;
            }
        } else {
            story = new Story();
        }

        // 1. Lưu Category
        Set<Category> storyCategories = new HashSet<>();
        if (apiStory.getCategory() != null) {
            for (OtruyenDto.CategoryItem catDto : apiStory.getCategory()) {
                Category cat = categoryRepository.findBySlug(catDto.getSlug())
                        .orElseGet(() -> categoryRepository.save(Category.builder()
                                .id(catDto.getId())
                                .name(catDto.getName())
                                .slug(catDto.getSlug())
                                .build()));
                storyCategories.add(cat);
            }
        }

        // 2. Lưu thông tin Story
        story.setId(apiStory.getId());
        story.setName(apiStory.getName());
        story.setSlug(apiStory.getSlug());
        story.setDescription(apiStory.getContent() != null ? apiStory.getContent() : ""); // Mô tả truyện
        story.setStatus(apiStory.getStatus());
        story.setThumbUrl(apiStory.getThumbUrl());
        story.setAuthor(apiStory.getAuthor() != null ? String.join(", ", apiStory.getAuthor()) : "Updating");
        story.setOriginUpdatedAt(apiUpdatedTime);
        story.setLastSyncedAt(LocalDateTime.now());
        story.setCategories(storyCategories);

        story = storyRepository.save(story);

        // 3. Upsert Chapters Batch Optimization
        if (apiStory.getChapters() != null && !apiStory.getChapters().isEmpty()) {
            List<OtruyenDto.ChapterItem> chapters = apiStory.getChapters().get(0).getServerData();
            List<Chapter> chaptersToSave = new ArrayList<>();
            
            for (OtruyenDto.ChapterItem chDto : chapters) {
                String chapterName = chDto.getChapterName();
                Optional<Chapter> chOptional = chapterRepository.findByStoryIdAndChapterName(story.getId(), chapterName);
                if (chOptional.isEmpty()) {
                    Chapter newChap = Chapter.builder()
                            .id(chDto.getChapterApiData().substring(chDto.getChapterApiData().lastIndexOf('/') + 1))
                            .story(story)
                            .chapterName(chapterName)
                            .chapterTitle(chDto.getChapterTitle())
                            .chapterApiData(chDto.getChapterApiData())
                            .isContentSynced(false)
                            .build();

                    // Lấy mảng ảnh JSON cho chapter mới này ngay lập tức
                    newChap = fetchChapterImages(newChap);
                    chaptersToSave.add(newChap);
                    
                    // Vẫn giữ lại Sleep nhẹ để bảo vệ Otruyen khỏi việc bị ta DDoS lúc cào ảnh
                    try { Thread.sleep(200); } catch(Exception ignored) {}
                }
            }
            // Batched Insert để tăng tốc độ ghi DB gấp hàng chục lần
            if (!chaptersToSave.isEmpty()) {
                chapterRepository.saveAll(chaptersToSave);
                log.info("🔥 BATCH MỚI: Đã insert đồng thời {} chapters mới cho truyện {}", chaptersToSave.size(), story.getName());
            }
        }
        
        log.info("Successfully synced story: {}", story.getName());
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    /**
     * Gọi API chi tiết của Chapter để lấy danh sách ảnh
     */
    private Chapter fetchChapterImages(Chapter chapter) {
        if (chapter.getChapterApiData() == null || chapter.getChapterApiData().isEmpty()) {
            return chapter;
        }
        try {
            ResponseEntity<OtruyenDto.BaseResponse<OtruyenDto.ChapterDetailData>> response = restTemplate.exchange(
                    chapter.getChapterApiData(), HttpMethod.GET, null,
                    new ParameterizedTypeReference<OtruyenDto.BaseResponse<OtruyenDto.ChapterDetailData>>() {}
            );

            if (response.getBody() != null && "success".equals(response.getBody().getStatus())) {
                OtruyenDto.ChapterDetailData data = response.getBody().getData();
                if (data != null && data.getItem() != null && data.getItem().getChapter_image() != null) {
                    String domain = data.getDomain_cdn();
                    String path = data.getItem().getChapter_path();

                    List<Map<String, Object>> imagesJsonList = new ArrayList<>();
                    for (OtruyenDto.ChapterImage img : data.getItem().getChapter_image()) {
                        Map<String, Object> imgMap = new HashMap<>();
                        imgMap.put("page", img.getImage_page());
                        imgMap.put("url", domain + "/" + path + "/" + img.getImage_file());
                        imagesJsonList.add(imgMap);
                    }
                    
                    chapter.setImagesJson(imagesJsonList);
                    chapter.setIsContentSynced(true);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy ảnh cho chapter API {}: {}", chapter.getChapterApiData(), e.getMessage());
        }
        return chapter;
    }
}
