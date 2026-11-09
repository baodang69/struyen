package com.struyen.struyen.services;

import com.struyen.struyen.dtos.MangaDexDto;
import com.struyen.struyen.entities.Category;
import com.struyen.struyen.entities.Chapter;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.repositories.CategoryRepository;
import com.struyen.struyen.repositories.ChapterRepository;
import com.struyen.struyen.repositories.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MangaDexSyncService {

    private final RestTemplate restTemplate;
    private final StoryRepository storyRepository;
    private final CategoryRepository categoryRepository;
    private final ChapterRepository chapterRepository;

    private static final String API_BASE = "https://api.mangadex.org";
    private static final String MANGADEX_USER_AGENT = "StruyenReader/1.0 (https://github.com/struyen/struyen_be; contact@struyen.com)";
    private static final String COVER_BASE_URL = "https://uploads.mangadex.org/covers";
    private static final String CDN_DOMAIN_MANGADEX = "https://uploads.mangadex.org";

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Dong bo danh sach truyen Tieng Viet pho bien nhat tu MangaDex (chay ngam bat dong bo).
     */
    @org.springframework.scheduling.annotation.Async
    public void syncPopularVietnameseMangaAsync(int limit, int offset) {
        log.info("[Async Task] Bat dau tien trinh chay ngam dong bo MangaDex limit={}, offset={}", limit, offset);
        syncPopularVietnameseManga(limit, offset);
        log.info("[Async Task] Hoan tat tien trinh chay ngam dong bo MangaDex.");
    }

    /**
     * Dong bo danh sach truyen Tieng Viet pho bien nhat tu MangaDex.
     */
    public int syncPopularVietnameseManga(int limit, int offset) {
        log.info("Bat dau dong bo MangaDex truyen Tieng Viet: limit={}, offset={}", limit, offset);

        String url = String.format(
                "%s/manga?limit=%d&offset=%d&availableTranslatedLanguage[]=vi&includes[]=cover_art&includes[]=author&order[followedCount]=desc&contentRating[]=safe&contentRating[]=suggestive",
                API_BASE, limit, offset
        );

        HttpHeaders headers = createMangaDexHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        int syncedCount = 0;
        try {
            ResponseEntity<MangaDexDto.MangaListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MangaDexDto.MangaListResponse.class
            );

            if (response.getBody() == null || response.getBody().getData() == null) {
                log.warn("MangaDex API tra ve response rong hoac null.");
                return 0;
            }

            List<MangaDexDto.MangaData> mangaList = response.getBody().getData();
            for (MangaDexDto.MangaData mangaData : mangaList) {
                try {
                    throttle();
                    syncSingleManga(mangaData);
                    syncedCount++;
                } catch (Exception e) {
                    log.error("Loi khi dong bo manga MangaDex id {}: {}", mangaData.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Loi khi goi MangaDex manga list API: {}", e.getMessage());
        }

        log.info("Hoan tat dong bo MangaDex: Da xu ly {} truyen.", syncedCount);
        return syncedCount;
    }

    /**
     * Dong bo mot truyen cu the theo MangaDex UUID.
     */
    public boolean syncMangaById(String mangaId) {
        log.info("Dong bo chi tiet truyen MangaDex id: {}", mangaId);
        String url = String.format("%s/manga/%s?includes[]=cover_art&includes[]=author", API_BASE, mangaId);

        HttpHeaders headers = createMangaDexHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<MangaDexDto.MangaListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MangaDexDto.MangaListResponse.class
            );

            if (response.getBody() != null && response.getBody().getData() != null && !response.getBody().getData().isEmpty()) {
                syncSingleManga(response.getBody().getData().get(0));
                return true;
            }
        } catch (Exception e) {
            log.error("Loi khi dong bo manga id {}: {}", mangaId, e.getMessage());
        }
        return false;
    }

    /**
     * Dong bo mot MangaData: Luu Story, Category va Chapter vao database.
     */
    @Transactional
    public Story syncSingleManga(MangaDexDto.MangaData mangaData) {
        String storyId = "md-" + mangaData.getId();
        String preferredTitle = mangaData.getPreferredTitle();

        Story story = storyRepository.findById(storyId).orElse(new Story());
        story.setId(storyId);
        story.setName(preferredTitle);

        // Sinh slug on dinh va tranh trung lap
        String baseSlug = toSlug(preferredTitle);
        String slug = baseSlug;
        Optional<Story> existingBySlug = storyRepository.findBySlug(slug);
        if (existingBySlug.isPresent() && !existingBySlug.get().getId().equals(storyId)) {
            slug = baseSlug + "-" + mangaData.getId().substring(0, Math.min(8, mangaData.getId().length()));
        }
        story.setSlug(slug);

        story.setDescription(mangaData.getPreferredDescription());
        story.setStatus(mangaData.getAttributes() != null ? mangaData.getAttributes().getStatus() : "ongoing");

        String author = mangaData.getAuthorName();
        story.setAuthor(author != null && !author.isBlank() ? author : "Updating");

        String coverFileName = mangaData.getCoverFileName();
        if (coverFileName != null && !coverFileName.isBlank()) {
            story.setThumbUrl(String.format("%s/%s/%s.512.jpg", COVER_BASE_URL, mangaData.getId(), coverFileName));
        } else {
            story.setThumbUrl("");
        }
        story.setCdnDomain(CDN_DOMAIN_MANGADEX);

        if (mangaData.getAttributes() != null && mangaData.getAttributes().getUpdatedAt() != null) {
            story.setOriginUpdatedAt(parseIsoDate(mangaData.getAttributes().getUpdatedAt()));
        }
        story.setLastSyncedAt(LocalDateTime.now());

        // Dong bo the loai (Categories)
        Set<Category> categories = extractCategories(mangaData);
        story.setCategories(categories);

        Story savedStory = storyRepository.save(story);

        // Dong bo danh sach Chapter Tieng Viet
        syncChaptersForManga(savedStory, mangaData.getId());

        log.info("Dong bo thanh cong truyen MangaDex: '{}' (slug: {})", savedStory.getName(), savedStory.getSlug());
        return savedStory;
    }

    /**
     * Lay danh sach chapter Tieng Viet cua manga va luu vao DB.
     */
    public void syncChaptersForManga(Story story, String mangaDexId) {
        String url = String.format(
                "%s/manga/%s/feed?translatedLanguage[]=vi&order[chapter]=asc&limit=100&includes[]=scanlation_group",
                API_BASE, mangaDexId
        );

        HttpHeaders headers = createMangaDexHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            throttle();
            ResponseEntity<MangaDexDto.ChapterListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MangaDexDto.ChapterListResponse.class
            );

            if (response.getBody() == null || response.getBody().getData() == null) {
                return;
            }

            List<MangaDexDto.ChapterData> chapterList = response.getBody().getData();
            if (chapterList.isEmpty()) {
                return;
            }

            Set<String> existingChapterNames = chapterRepository.findChapterNamesByStoryId(story.getId());
            List<Chapter> chaptersToSave = new ArrayList<>();

            for (MangaDexDto.ChapterData chData : chapterList) {
                if (chData.getAttributes() == null) continue;

                String rawChapterName = chData.getAttributes().getChapter();
                String chapterName = (rawChapterName != null && !rawChapterName.isBlank()) ? rawChapterName : "1";

                if (existingChapterNames.contains(chapterName)) {
                    continue;
                }

                String chapterTitle = chData.getAttributes().getTitle();
                String groupName = chData.getScanlationGroupName();
                if (groupName != null && !groupName.isBlank()) {
                    chapterTitle = (chapterTitle != null && !chapterTitle.isBlank())
                            ? chapterTitle + " [" + groupName + "]"
                            : "[" + groupName + "]";
                }

                String chapterApiData = API_BASE + "/at-home/server/" + chData.getId();

                Chapter chapter = Chapter.builder()
                        .id("md-ch-" + chData.getId())
                        .story(story)
                        .chapterName(chapterName)
                        .chapterTitle(chapterTitle)
                        .chapterApiData(chapterApiData)
                        .isContentSynced(false)
                        .imagesJson(Collections.emptyList())
                        .build();

                chaptersToSave.add(chapter);
                existingChapterNames.add(chapterName);
            }

            if (!chaptersToSave.isEmpty()) {
                chapterRepository.saveAll(chaptersToSave);
                log.info("Luu thanh cong {} chapters Tieng Viet cho truyen '{}'", chaptersToSave.size(), story.getName());
            }

        } catch (Exception e) {
            log.error("Loi khi lay feed chapter cua manga {}: {}", mangaDexId, e.getMessage());
        }
    }

    /**
     * Goi API MangaDex At-Home server de lay danh sach link anh cua chapter.
     * Phuong thuc nay duoc goi On-Demand khi doc chuong hoac khi dong bo truoc.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Chapter fetchChapterPages(Chapter chapter) {
        if (chapter.getChapterApiData() == null || chapter.getChapterApiData().isBlank()) {
            return chapter;
        }

        log.info("On-Demand Loading: Lay danh sach trang cho chapter id: {}", chapter.getId());
        HttpHeaders headers = createMangaDexHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<MangaDexDto.AtHomeResponse> response = restTemplate.exchange(
                    chapter.getChapterApiData(),
                    HttpMethod.GET,
                    entity,
                    MangaDexDto.AtHomeResponse.class
            );

            if (response.getBody() != null && response.getBody().getChapter() != null) {
                String baseUrl = response.getBody().getBaseUrl();
                String hash = response.getBody().getChapter().getHash();
                List<String> pageFiles = response.getBody().getChapter().getData();

                if (pageFiles != null && !pageFiles.isEmpty()) {
                    List<Map<String, Object>> imagesJsonList = new ArrayList<>();
                    for (int i = 0; i < pageFiles.size(); i++) {
                        String pageUrl = baseUrl + "/data/" + hash + "/" + pageFiles.get(i);
                        Map<String, Object> imgMap = new HashMap<>();
                        imgMap.put("page", i + 1);
                        imgMap.put("url", pageUrl);
                        imagesJsonList.add(imgMap);
                    }

                    chapter.setImagesJson(imagesJsonList);
                    chapter.setIsContentSynced(true);
                    return chapterRepository.save(chapter);
                }
            }
        } catch (Exception e) {
            log.error("Loi khi goi MangaDex At-Home cho chapter {}: {}", chapter.getId(), e.getMessage());
        }

        return chapter;
    }

    /**
     * Trich xuat va tao Category neu chua ton tai.
     */
    private Set<Category> extractCategories(MangaDexDto.MangaData mangaData) {
        Set<Category> categories = new HashSet<>();
        if (mangaData.getAttributes() == null || mangaData.getAttributes().getTags() == null) {
            return categories;
        }

        for (MangaDexDto.Tag tag : mangaData.getAttributes().getTags()) {
            String tagName = tag.getTagName();
            if (tagName == null || tagName.isBlank()) continue;

            String slug = toSlug(tagName);
            Category category = categoryRepository.findBySlug(slug).orElseGet(() -> {
                Category newCat = Category.builder()
                        .id("cat-" + slug)
                        .name(tagName)
                        .slug(slug)
                        .build();
                return categoryRepository.save(newCat);
            });
            categories.add(category);
        }

        return categories;
    }

    private HttpHeaders createMangaDexHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, MANGADEX_USER_AGENT);
        headers.set(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }

    /**
     * Gian cach 250ms de tuan thu tuyet doi Rate Limit <= 5 req/s cua MangaDex.
     */
    private void throttle() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private LocalDateTime parseIsoDate(String isoDateStr) {
        try {
            return LocalDateTime.parse(isoDateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "untitled";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        String slug = withoutDiacritics.toLowerCase()
                .replace("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return slug.isEmpty() ? "untitled" : slug;
    }
}
