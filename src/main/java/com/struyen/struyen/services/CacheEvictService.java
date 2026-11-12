package com.struyen.struyen.services;

import com.struyen.struyen.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Service quan ly lam moi va xoa cache (Cache Invalidation / Eviction) tap trung.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheEvictService {

    private final CacheManager cacheManager;

    public void evictCategoriesCache() {
        clearCache(RedisConfig.CACHE_CATEGORIES);
        log.info("[Cache Evict] Da lam moi cache danh muc the loai.");
    }

    public void evictFeaturedStoriesCache() {
        clearCache(RedisConfig.CACHE_FEATURED_STORIES);
        log.info("[Cache Evict] Da lam moi cache truyen de cu trang chu.");
    }

    public void evictStoriesPageCache() {
        clearCache(RedisConfig.CACHE_STORIES_PAGE);
        log.info("[Cache Evict] Da lam moi cache phan trang danh sach truyen.");
    }

    public void evictStoryDetailCache(String slug) {
        if (slug == null || slug.isBlank()) return;
        Cache cache = cacheManager.getCache(RedisConfig.CACHE_STORY_DETAIL);
        if (cache != null) {
            try {
                cache.evict(slug);
                log.info("[Cache Evict] Da xoa cache chi tiet truyen: {}", slug);
            } catch (Exception e) {
                log.warn("[Cache Evict Failed] Khong the xoa cache slug '{}': {}", slug, e.getMessage());
            }
        }
    }

    public void evictAllStoryCaches() {
        clearCache(RedisConfig.CACHE_FEATURED_STORIES);
        clearCache(RedisConfig.CACHE_STORIES_PAGE);
        clearCache(RedisConfig.CACHE_STORY_DETAIL);
        log.info("[Cache Evict] Da lam moi toan bo cache truyen (Featured, Pages, Detail).");
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            try {
                cache.clear();
            } catch (Exception e) {
                log.warn("[Cache Clear Failed] Khong the don dep cache '{}': {}", cacheName, e.getMessage());
            }
        }
    }
}
