package com.struyen.struyen.services;

import com.struyen.struyen.dtos.*;
import com.struyen.struyen.entities.Category;
import com.struyen.struyen.entities.Chapter;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.repositories.CategoryRepository;
import com.struyen.struyen.repositories.ChapterRepository;
import com.struyen.struyen.repositories.StoryRepository;
import com.struyen.struyen.utils.ChapterComparator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicStoryService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final CategoryRepository categoryRepository;
    private final MangaDexSyncService mangaDexSyncService;

    /**
     * Lay danh sach truyen co phan trang, tim kiem va loc theo the loai.
     * Ho tro loc linh hoat theo slug tieng Anh, ten Tieng Viet hoac slug Tieng Viet khong dau.
     */
    @Transactional(readOnly = true)
    public PageResponse<StoryListDto> getStories(int page, int size, String category) {
        return getStories(page, size, category, "newest", null, null);
    }

    /**
     * Lay danh sach truyen co phan trang, sap xep, tim kiem tu khoa va loc theo the loai tren Server-side.
     */
    @Transactional(readOnly = true)
    public PageResponse<StoryListDto> getStories(int page, int size, String category, String sort, String q) {
        return getStories(page, size, category, sort, q, null);
    }

    /**
     * Lay danh sach truyen co phan trang, sap xep, tim kiem tu khoa, loc the loai va loc trang thai tien do (status).
     */
    @Transactional(readOnly = true)
    public PageResponse<StoryListDto> getStories(int page, int size, String category, String sort, String q, String status) {
        Sort sortObj;
        String sortType = sort != null ? sort.trim().toLowerCase() : "newest";
        switch (sortType) {
            case "popular":
                sortObj = Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "updatedAt"));
                break;
            case "a-z":
                sortObj = Sort.by(Sort.Direction.ASC, "name");
                break;
            case "rating":
                sortObj = Sort.by(Sort.Direction.DESC, "viewCount");
                break;
            case "newest":
            default:
                sortObj = Sort.by(Sort.Direction.DESC, "updatedAt");
                break;
        }

        int springPage = page > 0 ? page - 1 : 0;
        int pageSize = size > 0 ? size : 12;
        Pageable pageable = PageRequest.of(springPage, pageSize, sortObj);

        final String cleanCategory = (category != null && !category.isBlank() && !category.equalsIgnoreCase("Tất cả") && !category.equalsIgnoreCase("all"))
                ? category.trim()
                : null;
        final String cleanKeyword = (q != null && !q.isBlank()) ? q.trim() : null;
        final String cleanStatus = (status != null && !status.isBlank() && !status.equalsIgnoreCase("Tất cả") && !status.equalsIgnoreCase("all"))
                ? status.trim().toLowerCase()
                : null;

        // Tim kiem thong tin Category phu hop neu co loc
        String targetCategoryId = null;
        String targetCategorySlug = null;
        String targetCategoryName = null;
        String targetCategoryNameVi = null;

        if (cleanCategory != null) {
            String slugifiedInput = MangaDexSyncService.toSlug(cleanCategory);
            List<Category> allCategories = categoryRepository.findAll();
            Category matched = allCategories.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(cleanCategory)
                            || (c.getNameVi() != null && c.getNameVi().equalsIgnoreCase(cleanCategory))
                            || (c.getNameVi() != null && MangaDexSyncService.toSlug(c.getNameVi()).equalsIgnoreCase(slugifiedInput))
                            || c.getSlug().equalsIgnoreCase(cleanCategory)
                            || MangaDexSyncService.toSlug(c.getName()).equalsIgnoreCase(slugifiedInput)
                            || c.getSlug().equalsIgnoreCase(slugifiedInput)
                            || c.getId().equalsIgnoreCase(cleanCategory))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                targetCategoryId = matched.getId();
                targetCategorySlug = matched.getSlug();
                targetCategoryName = matched.getName();
                targetCategoryNameVi = matched.getNameVi();
            } else {
                targetCategorySlug = cleanCategory;
            }
        }

        final String finalCategoryId = targetCategoryId;
        final String finalCategorySlug = targetCategorySlug;
        final String finalCategoryName = targetCategoryName;
        final String finalCategoryNameVi = targetCategoryNameVi;

        Specification<Story> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (cleanStatus != null) {
                predicates.add(cb.equal(cb.lower(root.get("status")), cleanStatus));
            }

            if (cleanCategory != null) {
                Join<Story, Category> catJoin = root.join("categories", JoinType.INNER);
                List<Predicate> catPreds = new ArrayList<>();
                if (finalCategoryId != null) {
                    catPreds.add(cb.equal(catJoin.get("id"), finalCategoryId));
                }
                if (finalCategorySlug != null) {
                    catPreds.add(cb.equal(cb.lower(catJoin.get("slug")), finalCategorySlug.toLowerCase()));
                }
                if (finalCategoryName != null) {
                    catPreds.add(cb.equal(cb.lower(catJoin.get("name")), finalCategoryName.toLowerCase()));
                }
                if (finalCategoryNameVi != null) {
                    catPreds.add(cb.equal(cb.lower(catJoin.get("nameVi")), finalCategoryNameVi.toLowerCase()));
                }
                if (catPreds.isEmpty()) {
                    catPreds.add(cb.equal(cb.lower(catJoin.get("slug")), cleanCategory.toLowerCase()));
                    catPreds.add(cb.equal(cb.lower(catJoin.get("name")), cleanCategory.toLowerCase()));
                    catPreds.add(cb.equal(cb.lower(catJoin.get("nameVi")), cleanCategory.toLowerCase()));
                }
                predicates.add(cb.or(catPreds.toArray(new Predicate[0])));
            }

            if (cleanKeyword != null) {
                String pattern = "%" + cleanKeyword.toLowerCase() + "%";
                Predicate matchName = cb.like(cb.lower(root.get("name")), pattern);
                Predicate matchSlug = cb.like(cb.lower(root.get("slug")), pattern);
                Predicate matchAuthor = cb.like(cb.lower(root.get("author")), pattern);
                predicates.add(cb.or(matchName, matchSlug, matchAuthor));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Story> storyPage = storyRepository.findAll(spec, pageable);
        Page<StoryListDto> dtoPage = storyPage.map(StoryListDto::fromEntity);
        return PageResponse.of(dtoPage);
    }

    /**
     * Lay danh sach truyen de cu (Featured Stories) cho Trang chu:
     * Uu tien truyen co isFeatured = true, sap xep theo featuredAt DESC (moi nhat len dau).
     * Neu so luong truyen de cu chua du slot, bo sung bang cac truyen co luot xem cao nhat.
     */
    @Transactional(readOnly = true)
    public List<StoryListDto> getFeaturedStories(int limit) {
        int maxLimit = (limit > 0 && limit <= 20) ? limit : 6;
        Pageable pageable = PageRequest.of(0, maxLimit);
        List<Story> featured = storyRepository.findFeaturedStories(pageable);
        if (featured.size() >= maxLimit) {
            return featured.stream().map(StoryListDto::fromEntity).toList();
        }

        List<Story> result = new ArrayList<>(featured);
        Set<String> existingIds = featured.stream().map(Story::getId).collect(Collectors.toSet());

        // Neu chua du slot de cu, bo sung bang cac truyen co luot doc cao nhat
        int remaining = maxLimit - result.size();
        Pageable popularPage = PageRequest.of(0, remaining + 10);
        List<Story> popular = storyRepository.findByIsActiveTrueOrderByViewCountDesc(popularPage);
        for (Story s : popular) {
            if (result.size() >= maxLimit) break;
            if (!existingIds.contains(s.getId())) {
                result.add(s);
                existingIds.add(s.getId());
            }
        }

        return result.stream().map(StoryListDto::fromEntity).toList();
    }

    /**
     * Lấy chi tiết thông tin truyện và toàn bộ danh sách chương theo slug.
     * Tự động làm sạch mô tả và kích hoạt tiến trình pre-fetch toàn bộ chương chưa đồng bộ.
     */
    @Transactional(readOnly = true)
    public StoryDetailDto getStoryDetail(String slug) {
        Story story = storyRepository.findDetailBySlug(slug)
                .filter(Story::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện với slug: " + slug));

        if (story.getDescription() != null && (story.getDescription().contains("%2") || story.getDescription().matches(".*[a-f0-9]{32}.*"))) {
            story.setDescription(MangaDexSyncService.cleanDescription(story.getDescription()));
        }

        List<Chapter> chapters = new ArrayList<>(chapterRepository.findByStoryIdOrderByCreatedAtAsc(story.getId()));
        chapters.sort(ChapterComparator.INSTANCE);
        List<ChapterShortDto> chapterDtos = chapters.stream()
                .map(ChapterShortDto::fromEntity)
                .toList();

        return StoryDetailDto.fromEntity(story, chapterDtos);
    }

    /**
     * Lấy chi tiết nội dung chương để đọc truyện:
     * Tự động chuyển đổi toàn bộ URL ảnh qua endpoint Image Proxy để tránh lỗi 403 CDN,
     * đồng thời tính toán liên kết chương trước (prev) và chương sau (next).
     */
    @Transactional(readOnly = true)
    public ChapterDetailDto getChapterDetail(String chapterId) {
        Chapter currentChapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương với id: " + chapterId));

        Story story = currentChapter.getStory();
        if (story == null || !Boolean.TRUE.equals(story.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Truyện chứa chương này đã bị ẩn hoặc không tồn tại.");
        }

        // Tính toán chương trước và chương sau dựa trên danh sách chương cùng truyện (đã sắp xếp tự nhiên)
        List<Chapter> storyChapters = new ArrayList<>(chapterRepository.findByStoryIdOrderByCreatedAtAsc(story.getId()));
        storyChapters.sort(ChapterComparator.INSTANCE);
        String prevChapterId = null;
        String nextChapterId = null;

        int currentIndex = -1;
        for (int i = 0; i < storyChapters.size(); i++) {
            if (storyChapters.get(i).getId().equals(chapterId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex > 0) {
            prevChapterId = storyChapters.get(currentIndex - 1).getId();
        }
        if (currentIndex >= 0 && currentIndex < storyChapters.size() - 1) {
            nextChapterId = storyChapters.get(currentIndex + 1).getId();
        }

        // On-demand fetch cho MangaDex chapter neu chua dong bo anh
        if ((currentChapter.getImagesJson() == null || currentChapter.getImagesJson().isEmpty())
                && currentChapter.getChapterApiData() != null
                && currentChapter.getChapterApiData().contains("mangadex.org/at-home/server")) {
            currentChapter = mangaDexSyncService.fetchChapterPages(currentChapter);
        }

        // Chuyển đổi danh sách ảnh raw sang Image Proxy URL
        List<String> proxyImages = new ArrayList<>();
        if (currentChapter.getImagesJson() != null) {
            for (Map<String, Object> imgMap : currentChapter.getImagesJson()) {
                Object rawUrlObj = imgMap.get("url");
                if (rawUrlObj != null) {
                    String rawUrl = rawUrlObj.toString();
                    String proxyUrl = "/api/v1/proxy/image?url=" + URLEncoder.encode(rawUrl, StandardCharsets.UTF_8);
                    proxyImages.add(proxyUrl);
                }
            }
        }

        return ChapterDetailDto.builder()
                .chapterId(currentChapter.getId())
                .storyId(story.getId())
                .storySlug(story.getSlug())
                .storyName(story.getName())
                .chapterName(currentChapter.getChapterName())
                .chapterTitle(currentChapter.getChapterTitle())
                .prevChapterId(prevChapterId)
                .nextChapterId(nextChapterId)
                .images(proxyImages)
                .build();
    }

    /**
     * Lay toan bo danh sach the loai truyen hien co trong he thong kem so luong truyen.
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        List<Object[]> rawList = categoryRepository.findAllCategoriesWithStoryCount();
        if (rawList != null && !rawList.isEmpty()) {
            List<CategoryDto> result = new ArrayList<>();
            for (Object[] row : rawList) {
                Category cat = (Category) row[0];
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                result.add(CategoryDto.fromEntity(cat, count));
            }
            return result;
        }

        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryDto::fromEntity)
                .toList();
    }
}
