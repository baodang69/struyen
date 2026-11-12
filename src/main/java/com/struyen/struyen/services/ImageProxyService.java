package com.struyen.struyen.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProxyService {

    private final RestTemplate restTemplate;

    // Thu muc Disk Cache de luu tru vinh vien anh that da tai ve
    private final Path cacheDir = Paths.get("data", "cache", "images");

    // Gioi han dung luong toi da cho mot file anh (15 MB) de chong tran bo nho Heap (OOM)
    private static final long MAX_IMAGE_SIZE_BYTES = 15 * 1024 * 1024;

    // Memory cache: Cache cac anh tai thanh cong (dung luong <= 1MB vi du anh bia) trong 1 gio
    public record CachedImage(byte[] data, MediaType contentType, long cachedAt) {}
    private final Map<String, CachedImage> imageMemoryCache = new ConcurrentHashMap<>();
    private static final long IMAGE_MEMORY_CACHE_TTL_MS = 60 * 60 * 1000L;
    private static final int MAX_MEMORY_CACHE_ENTRIES = 500;

    // Fallback SVG image when upstream CDN image cannot be retrieved
    private static final byte[] FALLBACK_SVG_BYTES = (
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"800\" viewBox=\"0 0 600 800\">" +
            "<rect width=\"600\" height=\"800\" fill=\"#F3F4F6\"/>" +
            "<rect x=\"250\" y=\"330\" width=\"100\" height=\"80\" rx=\"6\" fill=\"none\" stroke=\"#9CA3AF\" stroke-width=\"3\"/>" +
            "<circle cx=\"280\" cy=\"360\" r=\"10\" fill=\"none\" stroke=\"#9CA3AF\" stroke-width=\"3\"/>" +
            "<polyline points=\"258,402 285,375 305,390 325,370 342,385\" fill=\"none\" stroke=\"#9CA3AF\" stroke-width=\"3\" stroke-linejoin=\"round\"/>" +
            "<text x=\"300\" y=\"460\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"18\" font-weight=\"600\" fill=\"#6B7280\">KOMIKA</text>" +
            "<text x=\"300\" y=\"490\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"13\" fill=\"#9CA3AF\">IMAGE UNAVAILABLE</text>" +
            "</svg>"
    ).getBytes(StandardCharsets.UTF_8);

    // Whitelist cac domain CDN hop le cua Otruyen va MangaDex de ngan chan hoan toan SSRF
    private static final Set<String> ALLOWED_HOST_SUFFIXES = Set.of(
            "otruyencdn.com",
            "otruyenapi.com",
            "fffcdn.com",
            "mangadex.org",
            "mangadex.network"
    );

    private static final String MANGADEX_USER_AGENT = "StruyenReader/1.0 (https://github.com/struyen/struyen_be; contact@struyen.com)";
    private static final String DEFAULT_BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            log.error("Khong the khoi tao thu muc disk cache anh: {}", e.getMessage());
        }
    }

    public ResponseEntity<byte[]> fetchImage(String rawImageUrl) {
        String imageUrl = normalizeUrl(rawImageUrl);

        // 1. Kiem tra an toan bao mat SSRF (Validate Domain Whitelist & Protocol)
        if (!isAllowedUrl(imageUrl)) {
            log.warn("[SSRF Blocked] Tu choi truy cap URL khong thuoc whitelist: {}", imageUrl);
            return createFallbackResponse(HttpStatus.BAD_REQUEST);
        }

        // 2. Kiem tra In-Memory Cache (Phuc vu anh tuc thi neu da co trong RAM)
        CachedImage memoryCached = imageMemoryCache.get(imageUrl);
        if (memoryCached != null) {
            if (System.currentTimeMillis() - memoryCached.cachedAt() < IMAGE_MEMORY_CACHE_TTL_MS) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(memoryCached.contentType());
                responseHeaders.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
                return new ResponseEntity<>(memoryCached.data(), responseHeaders, HttpStatus.OK);
            } else {
                imageMemoryCache.remove(imageUrl);
            }
        }

        // 3. Kiem tra Disk Cache (Neu anh da luu tren o dia, doc va tra ve trong 1ms)
        String hash = hashUrl(imageUrl);
        Path dataFile = cacheDir.resolve(hash + ".data");
        Path typeFile = cacheDir.resolve(hash + ".type");
        try {
            if (Files.exists(dataFile) && Files.size(dataFile) > 0) {
                byte[] data = Files.readAllBytes(dataFile);
                String typeStr = Files.exists(typeFile) ? Files.readString(typeFile).trim() : "image/jpeg";
                MediaType mediaType = MediaType.parseMediaType(typeStr);

                // Cache vao RAM de lan tiep theo nhanh hon nua
                imageMemoryCache.put(imageUrl, new CachedImage(data, mediaType, System.currentTimeMillis()));

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(mediaType);
                responseHeaders.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
                return new ResponseEntity<>(data, responseHeaders, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.warn("Loi doc disk cache cho anh {}: {}", imageUrl, e.getMessage());
        }

        // 4. Neu chua co trong cache, thuc hien tai anh thuc te tu CDN bang duoc
        byte[] body = null;
        MediaType contentType = MediaType.IMAGE_JPEG;

        // Xay dung danh sach URL thu nghiem (Uu tien anh goc; neu la cover MangaDex bi loi thi fallback sang .512.jpg va .256.jpg)
        List<String> urlsToTry = new ArrayList<>();
        urlsToTry.add(imageUrl);
        if (imageUrl.contains("uploads.mangadex.org/covers/") && !imageUrl.endsWith(".512.jpg") && !imageUrl.endsWith(".256.jpg")) {
            urlsToTry.add(imageUrl + ".512.jpg");
            urlsToTry.add(imageUrl + ".256.jpg");
        }

        for (String targetUrl : urlsToTry) {
            try {
                URI uri = URI.create(targetUrl);
                String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";

                HttpHeaders headers = new HttpHeaders();
                if (host.endsWith("mangadex.org") || host.endsWith("mangadex.network")) {
                    headers.set(HttpHeaders.USER_AGENT, MANGADEX_USER_AGENT);
                } else {
                    headers.set(HttpHeaders.USER_AGENT, DEFAULT_BROWSER_USER_AGENT);
                }
                headers.set(HttpHeaders.ACCEPT, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = restTemplate.exchange(
                        targetUrl,
                        HttpMethod.GET,
                        entity,
                        byte[].class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().length > 0) {
                    // Kiem tra kich thuoc anh chong OOM
                    if (response.getBody().length > MAX_IMAGE_SIZE_BYTES) {
                        log.warn("[OOM Protection] Tu choi anh co dung luong vuot nguong: {} bytes", response.getBody().length);
                        return createFallbackResponse(HttpStatus.PAYLOAD_TOO_LARGE);
                    }

                    body = response.getBody();
                    if (response.getHeaders().getContentType() != null) {
                        contentType = response.getHeaders().getContentType();
                    }
                    log.info("Da lay thanh cong anh that tu CDN: {} ({} bytes)", targetUrl, body.length);
                    break;
                }
            } catch (Exception e) {
                log.warn("Thu lay anh that tu {} that bai: {}. Thu phuong an tiep theo...", targetUrl, e.getMessage());
            }
        }

        // 5. Luu vao ca Disk Cache va Memory Cache khi lay thanh cong anh that
        if (body != null) {
            try {
                Files.createDirectories(cacheDir);
                Files.write(dataFile, body);
                Files.writeString(typeFile, contentType.toString());
            } catch (Exception e) {
                log.warn("Khong the ghi disk cache cho anh {}: {}", imageUrl, e.getMessage());
            }

            if (body.length <= 1024 * 1024) {
                if (imageMemoryCache.size() >= MAX_MEMORY_CACHE_ENTRIES) {
                    imageMemoryCache.clear();
                }
                imageMemoryCache.put(imageUrl, new CachedImage(body, contentType, System.currentTimeMillis()));
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(contentType);
            responseHeaders.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
            return new ResponseEntity<>(body, responseHeaders, HttpStatus.OK);
        }

        // Truong hop bat kha khang sau khi da thu moi bien phap van khong the ket noi toi CDN
        log.error("Khong the tai anh that cho URL: {} sau khi thu tat ca cac phuong an", imageUrl);
        return createFallbackResponse(HttpStatus.OK);
    }

    private String hashUrl(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(url.hashCode());
        }
    }

    public void clearCaches() {
        imageMemoryCache.clear();
        try {
            if (Files.exists(cacheDir)) {
                try (var stream = Files.list(cacheDir)) {
                    stream.forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {}
                    });
                }
            }
        } catch (Exception ignored) {}
    }

    private ResponseEntity<byte[]> createFallbackResponse(HttpStatus status) {
        HttpHeaders fallbackHeaders = new HttpHeaders();
        fallbackHeaders.setContentType(MediaType.parseMediaType("image/svg+xml"));
        fallbackHeaders.setCacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic());
        return new ResponseEntity<>(FALLBACK_SVG_BYTES, fallbackHeaders, HttpStatus.OK);
    }

    /**
     * Chuan hoa URL anh: neu la ten file tuong doi (vi du: ten-truyen-thumb.jpg), tu dong noi prefix CDN Otruyen.
     */
    private String normalizeUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }
        String trimmed = imageUrl.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            if (trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1);
            }
            if (!trimmed.startsWith("uploads/")) {
                trimmed = "uploads/comics/" + trimmed;
            }
            return "https://img.otruyenapi.com/" + trimmed;
        }
        return trimmed;
    }

    /**
     * Xac thuc URL nham chong tan cong SSRF (Server-Side Request Forgery).
     * Chi chap nhan giao thuc http/https va hostname thuoc danh sach domain cua Otruyen CDN.
     */
    private boolean isAllowedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            host = host.toLowerCase();

            // Chan tuyet doi localhost, loopback va private network
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1") ||
                host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("169.254.") || host.startsWith("172.")) {
                return false;
            }

            // Kiem tra xem host co thuoc whitelist khong
            for (String allowedSuffix : ALLOWED_HOST_SUFFIXES) {
                if (host.equals(allowedSuffix) || host.endsWith("." + allowedSuffix)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
