---
name: otruyen-sync-manager
description: >-
  Use this skill when managing, debugging, testing, or expanding the Otruyen API crawler, data ingestion, chapter synchronization, or Image Proxy service.
---

# Otruyen Sync & Image Proxy Manager Skill

This skill documents how to interact with, debug, and enhance the integration with the Otruyen API (`otruyenapi.com`) and its CDN.

## Key Components

1. **Otruyen API Endpoints:**
   - List latest stories: `GET https://otruyenapi.com/v1/api/danh-sach/truyen-moi?page={page}`
   - Story detail & chapters: `GET https://otruyenapi.com/v1/api/truyen/{slug}`
   - Chapter detail & images: `GET https://sv1.otruyencdn.com/v1/api/chapter/{chapter_api_data}`

2. **Sync Methods in `OtruyenSyncService`:**
   - `syncCatchUpStories()`: Compares `origin_updated_at` timestamps to only fetch stories that have changed since the last sync. Safe to run periodically.
   - `syncFullStoriesAsync(int startPage, int endPage)`: Executes an asynchronous batch crawl across page ranges. Run when seeding or rebuilding catalog data.
   - `syncStoryDetail(String slug)`: Syncs a single comic and its chapters by slug.

3. **Image Proxying via `ImageProxyService`:**
   - Endpoint: `/api/v1/proxy/image?url={cdn_image_url}`
   - Otruyen CDN enforces hotlinking restrictions. The proxy injects `Referer: https://otruyenapi.com/` and `User-Agent`.
   - Browser caching header `Cache-Control: public, max-age=2592000` is attached.

## Testing & Troubleshooting Sync

### Triggering Manual Sync
```bash
curl -X POST "http://localhost:8080/api/v1/admin/sync/full?startPage=1&endPage=1"
```

### Checking Proxy Response
```bash
curl -I "http://localhost:8080/api/v1/proxy/image?url=https://sv1.otruyencdn.com/v1/api/chapter/xxx/001.jpg"
```
Expect `HTTP/1.1 200 OK` with `Content-Type: image/...`. If `403 Forbidden` is returned from CDN, verify the `Referer` header in `ImageProxyService.java`.

