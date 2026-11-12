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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtruyenSyncService {

    private final RestTemplate restTemplate;
    private final StoryRepository storyRepository;
    private final CategoryRepository categoryRepository;
    private final ChapterRepository chapterRepository;
    private final CacheEvictService cacheEvictService;
    private final AtomicBoolean isFullSyncRunning = new AtomicBoolean(false);

    public boolean isSyncRunning() {
        return isFullSyncRunning.get();
    }

    private static final String API_NEW_STORIES = "https://otruyenapi.com/v1/api/danh-sach/truyen-moi?page=";
    private static final String API_STORY_DETAIL = "https://otruyenapi.com/v1/api/truyen-tranh/";

    /**
     * Đồng bộ danh sách truyện mới báo cáo trên API (theo page)
     */
    public void syncNewStories(int page) {
        log.info("Started syncing new stories at page {}", page);
        String url = API_NEW_STORIES + page;
        try {
            ResponseEntity<OtruyenDto.BaseResponse<OtruyenDto.ListData>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<OtruyenDto.BaseResponse<OtruyenDto.ListData>>() {}
            );

            if (response.getBody() != null && "success".equals(response.getBody().getStatus())) {
                List<OtruyenDto.StoryItem> items = response.getBody().getData().getItems();
                if (items == null || items.isEmpty()) {
                    return;
                }

                // 1. Áp dụng Bộ lọc Tiền Tuyến (Pre-filtering)
                List<String> slugs = items.stream().map(OtruyenDto.StoryItem::getSlug).toList();
                List<Story> existingStories = storyRepository.findBySlugIn(slugs);
                Map<String, LocalDateTime> currentDates = existingStories.stream()
                        .filter(s -> s.getOriginUpdatedAt() != null)
                        .collect(Collectors.toMap(Story::getSlug, Story::getOriginUpdatedAt));

                for (OtruyenDto.StoryItem item : items) {
                    try {
                        LocalDateTime apiTime = parseDate(item.getUpdatedAt());
                        LocalDateTime dbTime = currentDates.get(item.getSlug());

                        // CẮT BỎ LUỒNG I/O NẾU DỮ LIỆU ĐÃ MỚI NHẤT
                        if (dbTime != null && apiTime != null && !apiTime.isAfter(dbTime)) {
                            log.info("Toi uu Filter: Bo qua truyen {} vi da up-to-date", item.getSlug());
                            continue;
                        }
                        syncStoryDetail(item.getSlug());
                    } catch (Exception e) {
                        log.error("Error syncing story slug {}: {}", item.getSlug(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi load danh sách page {}: {}", page, e.getMessage());
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
                        .collect(Collectors.toMap(Story::getSlug, Story::getOriginUpdatedAt));

                for (OtruyenDto.StoryItem chDto : items) {
                    LocalDateTime itemTime = parseDate(chDto.getUpdatedAt());

                    // Nếu không parse được ngày, vẫn tiếp tục đồng bộ an toàn
                    if (itemTime == null) {
                        syncStoryDetail(chDto.getSlug());
                        continue;
                    }

                    // Nếu thời gian cập nhật của truyện trên API CŨ HƠN mốc truyện mới nhất của chúng ta
                    if (itemTime.isBefore(lastSyncTime)) {
                        log.info("Đã lùi đến mốc thời gian an toàn (truyện cũ mốc {}). Kết thúc Catch-Up Sync tại trang {}.", lastSyncTime, page);
                        isUpToDate = true;
                        break;
                    }

                    LocalDateTime dbTime = currentDates.get(chDto.getSlug());
                    if (dbTime != null && !itemTime.isAfter(dbTime)) {
                        log.info("Toi uu Filter CatchUp: Bo qua truyen {} vi da up-to-date", chDto.getSlug());
                        continue;
                    }

                    // Ngược lại, nếu truyện trên API mới hơn, xử lý ngay:
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
    @Async("taskExecutor")
    public void syncFullStoriesAsync(int startPage, int endPage) {
        if (!isFullSyncRunning.compareAndSet(false, true)) {
            log.warn("Tien trinh cao du lieu Otruyen dang chay, tu choi yeu cau moi.");
            return;
        }
        try {
            log.info("--- BẮT ĐẦU QUÁ TRÌNH CÀO FULL DATA TỪ TRANG {} ĐẾN {} ---", startPage, endPage);
            for (int i = startPage; i <= endPage; i++) {
                try {
                    syncNewStories(i);
                    log.info("Đã quét xong trang {}. Đang đợi 2 giây để tránh bị chặn IP...", i);
                    Thread.sleep(2000);
                } catch (Exception e) {
                    log.error("Lỗi khi cào dữ liệu tại trang {}: {}", i, e.getMessage());
                }
            }
            log.info("--- HOÀN TẤT QUÁ TRÌNH CÀO FULL DATA ---");
            cacheEvictService.evictAllStoryCaches();
        } finally {
            isFullSyncRunning.set(false);
        }
    }

    /**
     * Đồng bộ chi tiết một truyện bằng slug.
     * Lưu ý QUAN TRỌNG: KHÔNG gắn @Transactional ở method này vì bên trong có gọi HTTP ngoài
     * và Thread.sleep. Việc giữ transaction sẽ làm cạn kiệt Database Connection Pool.
     */
    public void syncStoryDetail(String slug) {
        String url = API_STORY_DETAIL + slug;
        ResponseEntity<OtruyenDto.BaseResponse<OtruyenDto.DetailData>> response;
        try {
            response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<OtruyenDto.BaseResponse<OtruyenDto.DetailData>>() {}
            );
        } catch (Exception e) {
            log.error("Lỗi mạng khi tải chi tiết truyện {}: {}", slug, e.getMessage());
            return;
        }

        if (response.getBody() == null || !"success".equals(response.getBody().getStatus())) {
            return;
        }

        OtruyenDto.StoryItem apiStory = response.getBody().getData().getItem();
        LocalDateTime apiUpdatedTime = parseDate(apiStory.getUpdatedAt());

        Optional<Story> existingOptional = storyRepository.findBySlug(slug);
        Story story;

        if (existingOptional.isPresent()) {
            story = existingOptional.get();
            // Nếu truyện cũ lấy trên DB có ngày cập nhật >= API -> bỏ qua không sync lại
            if (story.getOriginUpdatedAt() != null && apiUpdatedTime != null &&
                    !apiUpdatedTime.isAfter(story.getOriginUpdatedAt())) {
                log.info("Story {} is already up to date. Skipping.", slug);
                return;
            }
        } else {
            story = new Story();
        }

        // 1. Phân giải Category
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

        // 2. Cập nhật thông tin Story và lưu vào Database trong transaction ngắn
        String cdnDomain = response.getBody().getData().getAppDomainCdnImage();
        if (cdnDomain == null || cdnDomain.isBlank()) {
            cdnDomain = "https://img.otruyenapi.com/uploads/comics";
        }

        story.setId(apiStory.getId());
        story.setName(apiStory.getName());
        story.setSlug(apiStory.getSlug());
        story.setDescription(apiStory.getContent() != null ? apiStory.getContent() : "");
        story.setStatus(apiStory.getStatus());
        story.setThumbUrl(apiStory.getThumbUrl());
        story.setCdnDomain(cdnDomain);
        story.setAuthor(apiStory.getAuthor() != null ? String.join(", ", apiStory.getAuthor()) : "Updating");
        story.setOriginUpdatedAt(apiUpdatedTime);
        story.setLastSyncedAt(LocalDateTime.now());
        story.setCategories(storyCategories);

        story = persistStory(story);

        // 3. Tối ưu N+1 Query: Lấy toàn bộ tên chapter hiện có trong DB qua 1 câu query duy nhất
        Set<String> existingChapterNames = (story.getId() != null)
                ? chapterRepository.findChapterNamesByStoryId(story.getId())
                : Collections.emptySet();

        // 4. Thu thập và tải ảnh cho các Chapter mới (Thực hiện ngoài Transaction)
        if (apiStory.getChapters() != null && !apiStory.getChapters().isEmpty()) {
            List<OtruyenDto.ChapterItem> chapters = apiStory.getChapters().get(0).getServerData();
            List<Chapter> chaptersToSave = new ArrayList<>();

            for (OtruyenDto.ChapterItem chDto : chapters) {
                String chapterName = chDto.getChapterName();

                // Kiểm tra O(1) trong bộ nhớ, loại bỏ hoàn toàn N+1 queries
                if (existingChapterNames.contains(chapterName)) {
                    continue;
                }

                Chapter newChap = Chapter.builder()
                        .id(chDto.getChapterApiData().substring(chDto.getChapterApiData().lastIndexOf('/') + 1))
                        .story(story)
                        .chapterName(chapterName)
                        .chapterTitle(chDto.getChapterTitle())
                        .chapterApiData(chDto.getChapterApiData())
                        .isContentSynced(false)
                        .build();

                // Tải danh sách ảnh qua API ngoài (Network I/O)
                newChap = fetchChapterImages(newChap);
                chaptersToSave.add(newChap);

                // Giãn cách nhẹ để tránh spam Otruyen CDN
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            // 5. Lưu hàng loạt Chapters trong một Transaction ngắn duy nhất
            if (!chaptersToSave.isEmpty()) {
                persistChaptersBatch(chaptersToSave);
                log.info("Batch moi: Da insert dong thoi {} chapters moi cho truyen {}", chaptersToSave.size(), story.getName());
            }
        }

        log.info("Successfully synced story: {}", story.getName());
    }

    /**
     * Giao dịch ngắn: Lưu thông tin Story
     */
    @Transactional
    public Story persistStory(Story story) {
        return storyRepository.save(story);
    }

    /**
     * Giao dịch ngắn: Lưu danh sách Chapter hàng loạt (Batch Insert)
     */
    @Transactional
    public void persistChaptersBatch(List<Chapter> chapters) {
        chapterRepository.saveAll(chapters);
    }

    /**
     * Parse thời gian từ API, trả về null nếu thất bại thay vì LocalDateTime.now()
     * để tránh làm lệch mốc đồng bộ (Catch-up sync).
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Không thể parse ngày ISO_DATE_TIME từ API: '{}', trả về null để bảo toàn mốc so sánh", dateStr);
            return null;
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
