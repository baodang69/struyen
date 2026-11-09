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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private static final Map<String, String> VIETNAMESE_TAG_MAP = Map.ofEntries(
            Map.entry("action", "Hành Động"),
            Map.entry("adventure", "Phiêu Lưu"),
            Map.entry("comedy", "Hài Hước"),
            Map.entry("drama", "Kịch Tính"),
            Map.entry("fantasy", "Giả Tưởng"),
            Map.entry("horror", "Kinh Dị"),
            Map.entry("mystery", "Bí Ẩn"),
            Map.entry("psychological", "Tâm Lý"),
            Map.entry("romance", "Lãng Mạn"),
            Map.entry("sci-fi", "Khoa Học Viễn Tưởng"),
            Map.entry("slice of life", "Đời Thường"),
            Map.entry("sports", "Thể Thao"),
            Map.entry("supernatural", "Siêu Nhiên"),
            Map.entry("tragedy", "Bi Kịch"),
            Map.entry("isekai", "Chuyển Sinh"),
            Map.entry("martial arts", "Võ Thuật"),
            Map.entry("school life", "Học Đường"),
            Map.entry("monsters", "Quái Vật"),
            Map.entry("demons", "Ma Quỷ"),
            Map.entry("magic", "Ma Pháp"),
            Map.entry("time travel", "Du Hành Thời Gian"),
            Map.entry("historical", "Lịch Sử"),
            Map.entry("award winning", "Đoạt Giải Thưởng"),
            Map.entry("reincarnation", "Trùng Sinh"),
            Map.entry("survival", "Sinh Tồn"),
            Map.entry("military", "Quân Sự"),
            Map.entry("superhero", "Siêu Anh Hùng"),
            Map.entry("villainess", "Ác Nữ"),
            Map.entry("virtual reality", "Thực Tế Ảo"),
            Map.entry("zombies", "Zombie"),
            Map.entry("vampires", "Ma Cà Rồng"),
            Map.entry("crime", "Tội Phạm"),
            Map.entry("detective", "Thám Tử"),
            Map.entry("cooking", "Ẩm Thực"),
            Map.entry("video games", "Trò Chơi Điện Tử"),
            Map.entry("aliens", "Người Ngoài Hành Tinh"),
            Map.entry("post-apocalyptic", "Hậu Tận Thế"),
            Map.entry("crossdressing", "Giả Trang"),
            Map.entry("gender bender", "Hoán Đổi Giới Tính")
    );

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

        // Uu tien mo ta Tieng Viet, neu chi co tieng Anh thi tu dong dich sang Tieng Viet
        String rawDescription = mangaData.getPreferredDescription();
        String viDescription = rawDescription;
        if (isMostlyEnglish(rawDescription)) {
            viDescription = translateToVietnamese(rawDescription);
        }
        story.setDescription(viDescription);

        story.setStatus(mangaData.getAttributes() != null ? mangaData.getAttributes().getStatus() : "ongoing");

        String author = mangaData.getAuthorName();
        story.setAuthor(author != null && !author.isBlank() ? author : "Đang cập nhật");

        // Luu anh bia goc MangaDex dam bao luon luon ton tai va fetch thanh cong
        String coverFileName = mangaData.getCoverFileName();
        if (coverFileName != null && !coverFileName.isBlank()) {
            story.setThumbUrl(String.format("%s/%s/%s", COVER_BASE_URL, mangaData.getId(), coverFileName));
        } else {
            story.setThumbUrl("");
        }
        story.setCdnDomain(CDN_DOMAIN_MANGADEX);

        if (mangaData.getAttributes() != null && mangaData.getAttributes().getUpdatedAt() != null) {
            story.setOriginUpdatedAt(parseIsoDate(mangaData.getAttributes().getUpdatedAt()));
        }
        story.setLastSyncedAt(LocalDateTime.now());

        // Dong bo the loai (Categories) voi ten Tieng Viet chuan
        Set<Category> categories = extractCategories(mangaData);
        story.setCategories(categories);

        Story savedStory = storyRepository.save(story);

        // Dong bo danh sach Chapter Tieng Viet kem prefetch anh 5 chuong dau tien
        syncChaptersForManga(savedStory, mangaData.getId());

        log.info("Dong bo thanh cong truyen MangaDex: '{}' (slug: {})", savedStory.getName(), savedStory.getSlug());
        return savedStory;
    }

    /**
     * Lay danh sach chapter Tieng Viet cua manga va luu vao DB.
     * Prefetch luon 5 chapter dau tien de nguoi doc mo chapter tuc thi (Zero Wait Time).
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
                List<Chapter> saved = chapterRepository.saveAll(chaptersToSave);
                log.info("Luu thanh cong {} chapters Tieng Viet cho truyen '{}'", saved.size(), story.getName());

                // Prefetch 5 chuong dau tien vao database de nguoi doc mo tuc thi trong vai ms
                int prefetchCount = Math.min(5, saved.size());
                for (int i = 0; i < prefetchCount; i++) {
                    try {
                        throttle();
                        fetchChapterPages(saved.get(i));
                    } catch (Exception e) {
                        log.warn("Loi prefetch chapter {} cho truyen {}: {}", saved.get(i).getChapterName(), story.getName(), e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Loi khi lay feed chapter cua manga {}: {}", mangaDexId, e.getMessage());
        }
    }

    /**
     * Goi API MangaDex At-Home server de lay danh sach link anh cua chapter.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Chapter fetchChapterPages(Chapter chapter) {
        if (chapter.getChapterApiData() == null || chapter.getChapterApiData().isBlank()) {
            return chapter;
        }

        log.info("Lay danh sach trang cho chapter id: {}", chapter.getId());
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
     * Tu dong dich mo ta sang tieng Viet bang Google Translate endpoint mien phi.
     */
    public String translateToVietnamese(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        try {
            String trimmed = text.length() > 1000 ? text.substring(0, 1000) : text;
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=vi&dt=t&q="
                    + URLEncoder.encode(trimmed, StandardCharsets.UTF_8);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getBody() != null && response.getBody().startsWith("[[[")) {
                String body = response.getBody();
                StringBuilder sb = new StringBuilder();
                int idx = 0;
                while ((idx = body.indexOf("[\"", idx)) != -1) {
                    int end = body.indexOf("\",\"", idx + 2);
                    if (end != -1) {
                        String sentence = body.substring(idx + 2, end);
                        sb.append(sentence).append(" ");
                        idx = end + 3;
                    } else {
                        break;
                    }
                }
                String result = sb.toString().trim();
                if (result.contains("%")) {
                    try {
                        result = URLDecoder.decode(result, StandardCharsets.UTF_8);
                    } catch (Exception ignored) {
                    }
                }
                result = result.replaceAll("\\b[a-f0-9]{32}\\b", "").trim();
                if (!result.isBlank()) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("Khong the dich mo ta sang tieng Viet: {}", e.getMessage());
        }
        return text;
    }

    private boolean isMostlyEnglish(String text) {
        if (text == null || text.isBlank()) return false;
        // Kiem tra neu khong chua bat ky ky tu co dau tieng Viet nao
        boolean hasVietnameseAccents = text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*");
        return !hasVietnameseAccents;
    }

    /**
     * Trich xuat va tao Category neu chua ton tai voi ten Tieng Viet chuan.
     */
    private Set<Category> extractCategories(MangaDexDto.MangaData mangaData) {
        Set<Category> categories = new HashSet<>();
        if (mangaData.getAttributes() == null || mangaData.getAttributes().getTags() == null) {
            return categories;
        }

        for (MangaDexDto.Tag tag : mangaData.getAttributes().getTags()) {
            String tagName = tag.getTagName();
            if (tagName == null || tagName.isBlank()) continue;

            String lower = tagName.trim().toLowerCase();
            String viName = VIETNAMESE_TAG_MAP.getOrDefault(lower, tagName);
            String slug = toSlug(tagName);

            Category category = categoryRepository.findBySlug(slug).orElseGet(() -> {
                Category newCat = Category.builder()
                        .id("cat-" + slug)
                        .name(viName)
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
