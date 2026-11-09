package com.struyen.struyen.services;

import com.struyen.struyen.dtos.MangaDexDto;
import com.struyen.struyen.entities.Category;
import com.struyen.struyen.entities.Chapter;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.repositories.CategoryRepository;
import com.struyen.struyen.repositories.ChapterRepository;
import com.struyen.struyen.repositories.StoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MangaDexSyncServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ChapterRepository chapterRepository;

    private MangaDexSyncService mangaDexSyncService;

    @BeforeEach
    void setUp() {
        mangaDexSyncService = new MangaDexSyncService(
                restTemplate,
                storyRepository,
                categoryRepository,
                chapterRepository
        );
    }

    @Test
    void shouldConvertVietnameseTitlesToSlug() {
        assertEquals("xom-than-tien", MangaDexSyncService.toSlug("Xóm Thần Tiên"));
        assertEquals("dau-la-dai-luc", MangaDexSyncService.toSlug("Đấu La Đại Lục"));
        assertEquals("chu-thuat-hoi-chien", MangaDexSyncService.toSlug("Chú Thuật Hồi Chiến"));
        assertEquals("untitled", MangaDexSyncService.toSlug("   "));
        assertEquals("untitled", MangaDexSyncService.toSlug(null));
    }

    @Test
    void shouldFetchChapterPagesOnDemand() {
        Chapter chapter = Chapter.builder()
                .id("md-ch-chapter-1")
                .chapterApiData("https://api.mangadex.org/at-home/server/chapter-1")
                .isContentSynced(false)
                .build();

        MangaDexDto.AtHomeResponse atHomeResponse = new MangaDexDto.AtHomeResponse();
        atHomeResponse.setBaseUrl("https://cmdxd98sb0x3yprd.mangadex.network");
        MangaDexDto.AtHomeChapter atHomeChapter = new MangaDexDto.AtHomeChapter();
        atHomeChapter.setHash("abc123hash");
        atHomeChapter.setData(List.of("page1.png", "page2.png"));
        atHomeResponse.setChapter(atHomeChapter);

        when(restTemplate.exchange(eq(chapter.getChapterApiData()), eq(HttpMethod.GET), any(), eq(MangaDexDto.AtHomeResponse.class)))
                .thenReturn(ResponseEntity.ok(atHomeResponse));
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chapter result = mangaDexSyncService.fetchChapterPages(chapter);

        assertTrue(result.getIsContentSynced());
        assertNotNull(result.getImagesJson());
        assertEquals(2, result.getImagesJson().size());
        assertEquals("https://cmdxd98sb0x3yprd.mangadex.network/data/abc123hash/page1.png", result.getImagesJson().get(0).get("url"));
        assertEquals(1, result.getImagesJson().get(0).get("page"));
        assertEquals("https://cmdxd98sb0x3yprd.mangadex.network/data/abc123hash/page2.png", result.getImagesJson().get(1).get("url"));
        assertEquals(2, result.getImagesJson().get(1).get("page"));
    }

    @Test
    void shouldSyncSingleMangaAndMapFieldsCorrectly() {
        MangaDexDto.MangaData mangaData = new MangaDexDto.MangaData();
        mangaData.setId("manga-uuid-123");

        MangaDexDto.MangaAttributes attributes = new MangaDexDto.MangaAttributes();
        attributes.setTitle(Map.of("en", "Hero Story"));
        attributes.setAltTitles(List.of(Map.of("vi", "Cau Chuyen Anh Hung")));
        attributes.setDescription(Map.of("vi", "Mo ta tieng Viet"));
        attributes.setStatus("ongoing");
        attributes.setUpdatedAt("2026-01-01T00:00:00");

        MangaDexDto.Tag tag = new MangaDexDto.Tag();
        MangaDexDto.TagAttributes tagAttr = new MangaDexDto.TagAttributes();
        tagAttr.setName(Map.of("en", "Action"));
        tag.setAttributes(tagAttr);
        attributes.setTags(List.of(tag));

        mangaData.setAttributes(attributes);

        MangaDexDto.Relationship coverRel = new MangaDexDto.Relationship();
        coverRel.setType("cover_art");
        coverRel.setAttributes(Map.of("fileName", "cover.jpg"));

        MangaDexDto.Relationship authorRel = new MangaDexDto.Relationship();
        authorRel.setType("author");
        authorRel.setAttributes(Map.of("name", "Oda Eiichiro"));

        mangaData.setRelationships(List.of(coverRel, authorRel));

        when(storyRepository.findById("md-manga-uuid-123")).thenReturn(Optional.empty());
        when(storyRepository.findBySlug("cau-chuyen-anh-hung")).thenReturn(Optional.empty());
        when(categoryRepository.findBySlug("action")).thenReturn(Optional.of(Category.builder().id("cat-action").name("Action").slug("action").build()));
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock empty feed for chapters
        MangaDexDto.ChapterListResponse feedResponse = new MangaDexDto.ChapterListResponse();
        feedResponse.setData(Collections.emptyList());
        when(restTemplate.exchange(contains("/feed"), eq(HttpMethod.GET), any(), eq(MangaDexDto.ChapterListResponse.class)))
                .thenReturn(ResponseEntity.ok(feedResponse));

        Story syncedStory = mangaDexSyncService.syncSingleManga(mangaData);

        assertEquals("md-manga-uuid-123", syncedStory.getId());
        assertEquals("Cau Chuyen Anh Hung", syncedStory.getName());
        assertEquals("cau-chuyen-anh-hung", syncedStory.getSlug());
        assertEquals("Mo ta tieng Viet", syncedStory.getDescription());
        assertEquals("Oda Eiichiro", syncedStory.getAuthor());
        assertEquals("https://uploads.mangadex.org/covers/manga-uuid-123/cover.jpg.512.jpg", syncedStory.getThumbUrl());
        assertEquals("https://uploads.mangadex.org", syncedStory.getCdnDomain());
        assertEquals(1, syncedStory.getCategories().size());
    }
}
