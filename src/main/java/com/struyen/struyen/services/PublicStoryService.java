package com.struyen.struyen.services;

import com.struyen.struyen.dtos.*;
import com.struyen.struyen.entities.Category;
import com.struyen.struyen.entities.Chapter;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.repositories.CategoryRepository;
import com.struyen.struyen.repositories.ChapterRepository;
import com.struyen.struyen.repositories.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicStoryService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final CategoryRepository categoryRepository;
    private final MangaDexSyncService mangaDexSyncService;

    /**
     * Lấy danh sách truyện có phân trang và lọc theo thể loại.
     */
    @Transactional(readOnly = true)
    public PageResponse<StoryListDto> getStories(int page, int size, String categorySlug) {
        int springPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(springPage, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Story> storyPage;
        if (categorySlug != null && !categorySlug.isBlank()) {
            storyPage = storyRepository.findByIsActiveTrueAndCategoriesSlug(categorySlug, pageable);
        } else {
            storyPage = storyRepository.findByIsActiveTrue(pageable);
        }

        Page<StoryListDto> dtoPage = storyPage.map(StoryListDto::fromEntity);
        return PageResponse.of(dtoPage);
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

        List<Chapter> chapters = chapterRepository.findByStoryIdOrderByCreatedAtAsc(story.getId());
        List<ChapterShortDto> chapterDtos = chapters.stream()
                .map(ChapterShortDto::fromEntity)
                .toList();

        // Kích hoạt tiến trình pre-fetch toàn bộ chương còn lại dưới nền
        long unsyncedCount = chapterRepository.countByStoryIdAndIsContentSyncedFalse(story.getId());
        if (unsyncedCount > 0) {
            mangaDexSyncService.prefetchAllRemainingChaptersAsync(story.getId());
        }

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

        // Tính toán chương trước và chương sau dựa trên danh sách chương cùng truyện
        List<Chapter> storyChapters = chapterRepository.findByStoryIdOrderByCreatedAtAsc(story.getId());
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
     * Lấy toàn bộ danh sách thể loại truyện hiện có trong hệ thống.
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryDto::fromEntity)
                .toList();
    }
}
