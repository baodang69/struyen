package com.struyen.struyen.services;

import com.struyen.struyen.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheEvictServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache categoriesCache;

    @Mock
    private Cache featuredStoriesCache;

    @Mock
    private Cache storiesPageCache;

    @Mock
    private Cache storyDetailCache;

    private CacheEvictService cacheEvictService;

    @BeforeEach
    void setUp() {
        cacheEvictService = new CacheEvictService(cacheManager);
    }

    @Test
    void shouldClearCategoriesCache() {
        when(cacheManager.getCache(RedisConfig.CACHE_CATEGORIES)).thenReturn(categoriesCache);

        cacheEvictService.evictCategoriesCache();

        verify(categoriesCache).clear();
    }

    @Test
    void shouldClearFeaturedStoriesCache() {
        when(cacheManager.getCache(RedisConfig.CACHE_FEATURED_STORIES)).thenReturn(featuredStoriesCache);

        cacheEvictService.evictFeaturedStoriesCache();

        verify(featuredStoriesCache).clear();
    }

    @Test
    void shouldEvictSpecificStoryDetailSlug() {
        when(cacheManager.getCache(RedisConfig.CACHE_STORY_DETAIL)).thenReturn(storyDetailCache);

        cacheEvictService.evictStoryDetailCache("one-piece");

        verify(storyDetailCache).evict("one-piece");
    }

    @Test
    void shouldClearAllStoryCaches() {
        when(cacheManager.getCache(RedisConfig.CACHE_FEATURED_STORIES)).thenReturn(featuredStoriesCache);
        when(cacheManager.getCache(RedisConfig.CACHE_STORIES_PAGE)).thenReturn(storiesPageCache);
        when(cacheManager.getCache(RedisConfig.CACHE_STORY_DETAIL)).thenReturn(storyDetailCache);

        cacheEvictService.evictAllStoryCaches();

        verify(featuredStoriesCache).clear();
        verify(storiesPageCache).clear();
        verify(storyDetailCache).clear();
    }

    @Test
    void shouldHandleNullOrMissingCacheGracefully() {
        when(cacheManager.getCache(anyString())).thenReturn(null);

        cacheEvictService.evictCategoriesCache();
        cacheEvictService.evictStoryDetailCache("unknown-slug");

        verify(cacheManager, times(2)).getCache(anyString());
    }
}
