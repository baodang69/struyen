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

    @Mock
    private CacheEvictService cacheEvictService;

    private MangaDexSyncService mangaDexSyncService;

    @BeforeEach
    void setUp() {
        mangaDexSyncService = new MangaDexSyncService(
                restTemplate,
                storyRepository,
                categoryRepository,
                chapterRepository,
                cacheEvictService
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
        MangaDexDto.MangaData mangaData = createMockMangaData();

        when(storyRepository.findById("md-manga-uuid-123")).thenReturn(Optional.empty());
        when(storyRepository.findBySlug("cau-chuyen-anh-hung")).thenReturn(Optional.empty());
        when(categoryRepository.findBySlug("action")).thenReturn(Optional.of(Category.builder().id("cat-action").name("Action").slug("action").build()));
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterRepository.countByStoryId("md-manga-uuid-123")).thenReturn(1L);

        MangaDexDto.ChapterData chData = new MangaDexDto.ChapterData();
        chData.setId("chapter-uuid-1");
        MangaDexDto.ChapterAttributes chAttr = new MangaDexDto.ChapterAttributes();
        chAttr.setChapter("1");
        chAttr.setTitle("Khoi dau");
        chAttr.setPublishAt("2026-01-01T00:00:00");
        chAttr.setPages(10);
        chData.setAttributes(chAttr);

        MangaDexDto.ChapterListResponse feedResponse = new MangaDexDto.ChapterListResponse();
        feedResponse.setTotal(1);
        feedResponse.setData(List.of(chData));

        when(restTemplate.exchange(contains("translatedLanguage[]=vi"), eq(HttpMethod.GET), any(), eq(MangaDexDto.ChapterListResponse.class)))
                .thenReturn(ResponseEntity.ok(feedResponse));

        Story syncedStory = mangaDexSyncService.syncSingleManga(mangaData);

        assertNotNull(syncedStory);
        assertEquals("md-manga-uuid-123", syncedStory.getId());
        assertEquals("Cau Chuyen Anh Hung", syncedStory.getName());
        assertEquals("cau-chuyen-anh-hung", syncedStory.getSlug());
        assertEquals("Mo ta tieng Viet", syncedStory.getDescription());
        assertEquals("Oda Eiichiro", syncedStory.getAuthor());
        assertEquals("https://uploads.mangadex.org/covers/manga-uuid-123/cover.jpg", syncedStory.getThumbUrl());
        assertEquals("https://uploads.mangadex.org", syncedStory.getCdnDomain());
        assertEquals("vi", syncedStory.getLanguage());
        assertTrue(syncedStory.getHasVietnameseTranslation());
        assertEquals(1, syncedStory.getCategories().size());
    }

    @Test
    void shouldFallbackToEnglishWhenNoVietnameseChapters() {
        MangaDexDto.MangaData mangaData = createMockMangaData();

        when(storyRepository.findById("md-manga-uuid-123")).thenReturn(Optional.empty());
        when(storyRepository.findBySlug("cau-chuyen-anh-hung")).thenReturn(Optional.empty());
        when(categoryRepository.findBySlug("action")).thenReturn(Optional.of(Category.builder().id("cat-action").name("Action").slug("action").build()));
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterRepository.countByStoryId("md-manga-uuid-123")).thenReturn(1L);

        // VI feed returns empty list
        MangaDexDto.ChapterListResponse emptyViFeed = new MangaDexDto.ChapterListResponse();
        emptyViFeed.setTotal(0);
        emptyViFeed.setData(Collections.emptyList());
        when(restTemplate.exchange(contains("translatedLanguage[]=vi"), eq(HttpMethod.GET), any(), eq(MangaDexDto.ChapterListResponse.class)))
                .thenReturn(ResponseEntity.ok(emptyViFeed));

        // EN feed returns 1 chapter
        MangaDexDto.ChapterData enChData = new MangaDexDto.ChapterData();
        enChData.setId("en-chapter-uuid-1");
        MangaDexDto.ChapterAttributes enChAttr = new MangaDexDto.ChapterAttributes();
        enChAttr.setChapter("1");
        enChAttr.setTitle("Beginning");
        enChAttr.setPublishAt("2026-01-01T00:00:00");
        enChAttr.setPages(10);
        enChData.setAttributes(enChAttr);

        MangaDexDto.ChapterListResponse enFeedResponse = new MangaDexDto.ChapterListResponse();
        enFeedResponse.setTotal(1);
        enFeedResponse.setData(List.of(enChData));
        when(restTemplate.exchange(contains("translatedLanguage[]=en"), eq(HttpMethod.GET), any(), eq(MangaDexDto.ChapterListResponse.class)))
                .thenReturn(ResponseEntity.ok(enFeedResponse));

        Story syncedStory = mangaDexSyncService.syncSingleManga(mangaData);

        assertNotNull(syncedStory);
        assertEquals("en", syncedStory.getLanguage());
        assertFalse(syncedStory.getHasVietnameseTranslation());
    }

    @Test
    void shouldDiscardStoryWhenBothVietnameseAndEnglishHaveZeroChapters() {
        MangaDexDto.MangaData mangaData = createMockMangaData();

        when(storyRepository.findById("md-manga-uuid-123")).thenReturn(Optional.empty());
        when(storyRepository.findBySlug("cau-chuyen-anh-hung")).thenReturn(Optional.empty());
        when(categoryRepository.findBySlug("action")).thenReturn(Optional.of(Category.builder().id("cat-action").name("Action").slug("action").build()));
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MangaDexDto.ChapterListResponse emptyFeed = new MangaDexDto.ChapterListResponse();
        emptyFeed.setData(Collections.emptyList());

        when(restTemplate.exchange(contains("translatedLanguage[]=vi"), eq(HttpMethod.GET), any(), eq(MangaDexDto.ChapterListResponse.class)))
                .thenReturn(ResponseEntity.ok(emptyFeed));
        when(restTemplate.exchange(contains("translatedLanguage[]=en"), eq(HttpMethod.GET), any(), eq(MangaDexDto.ChapterListResponse.class)))
                .thenReturn(ResponseEntity.ok(emptyFeed));

        Story syncedStory = mangaDexSyncService.syncSingleManga(mangaData);

        assertNull(syncedStory);
        verify(storyRepository, atLeastOnce()).delete(any(Story.class));
    }

    @Test
    void shouldIgnoreExternalAndEmptyChapters() {
        MangaDexDto.MangaData mangaData = createMockMangaData();

        when(storyRepository.findById("md-manga-uuid-123")).thenReturn(Optional.empty());
        when(storyRepository.findBySlug("cau-chuyen-anh-hung")).thenReturn(Optional.empty());
        when(categoryRepository.findBySlug("action")).thenReturn(Optional.of(Category.builder().id("cat-action").name("Action").slug("action").build()));
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // External chapter (e.g. Webnovel or K MANGA)
        MangaDexDto.ChapterData extCh = new MangaDexDto.ChapterData();
        extCh.setId("ext-chapter-1");
        MangaDexDto.ChapterAttributes extAttr = new MangaDexDto.ChapterAttributes();
        extAttr.setChapter("1");
        extAttr.setTitle("External Chapter");
        extAttr.setExternalUrl("https://webnovel.com/comic/123");
        extAttr.setPages(0);
        extCh.setAttributes(extAttr);

        MangaDexDto.ChapterListResponse feedResponse = new MangaDexDto.ChapterListResponse();
        feedResponse.setTotal(1);
        feedResponse.setData(List.of(extCh));

        when(restTemplate.exchange(contains("translatedLanguage[]=vi"), eq(HttpMethod.GET), any(), eq(MangaDexDto.ChapterListResponse.class)))
                .thenReturn(ResponseEntity.ok(feedResponse));
        when(restTemplate.exchange(contains("translatedLanguage[]=en"), eq(HttpMethod.GET), any(), eq(MangaDexDto.ChapterListResponse.class)))
                .thenReturn(ResponseEntity.ok(feedResponse));

        Story syncedStory = mangaDexSyncService.syncSingleManga(mangaData);

        // All chapters were external, so the story has 0 valid chapters and is discarded/deleted
        assertNull(syncedStory);
        verify(chapterRepository, never()).saveAll(anyList());
        verify(storyRepository, atLeastOnce()).delete(any(Story.class));
    }

    private MangaDexDto.MangaData createMockMangaData() {
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
        return mangaData;
    }
}

