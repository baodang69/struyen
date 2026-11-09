package com.struyen.struyen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProxyService {

    private final RestTemplate restTemplate;

    // Gioi han dung luong toi da cho mot file anh (15 MB) de chong tran bo nho Heap (OOM)
    private static final long MAX_IMAGE_SIZE_BYTES = 15 * 1024 * 1024;

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

    // Whitelist cac domain CDN hop le cua Otruyen de ngan chan hoan toan SSRF
    private static final Set<String> ALLOWED_HOST_SUFFIXES = Set.of(
            "otruyencdn.com",
            "otruyenapi.com",
            "fffcdn.com"
    );

    public ResponseEntity<byte[]> fetchImage(String rawImageUrl) {
        String imageUrl = normalizeUrl(rawImageUrl);

        // 1. Kiem tra an toan bao mat SSRF (Validate Domain Whitelist & Protocol)
        if (!isAllowedUrl(imageUrl)) {
            log.warn("[SSRF Blocked] Tu choi truy cap URL khong thuoc whitelist: {}", imageUrl);
            return createFallbackResponse(HttpStatus.BAD_REQUEST);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://otruyenapi.com/");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set(HttpHeaders.ACCEPT, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            // Kiem tra Content-Length de bao ve Heap RAM khoi OOM
            List<String> contentLengthHeaders = response.getHeaders().get(HttpHeaders.CONTENT_LENGTH);
            if (contentLengthHeaders != null && !contentLengthHeaders.isEmpty()) {
                long contentLength = Long.parseLong(contentLengthHeaders.get(0));
                if (contentLength > MAX_IMAGE_SIZE_BYTES) {
                    log.warn("[OOM Protection] Tu choi anh co dung luong vuot nguong: {} bytes", contentLength);
                    return createFallbackResponse(HttpStatus.PAYLOAD_TOO_LARGE);
                }
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null) {
                responseHeaders.setContentType(contentType);
            } else {
                responseHeaders.setContentType(MediaType.IMAGE_JPEG);
            }

            // Thiet lap Cache-Control de trinh duyet tu do cache trong 30 ngay (giup giam tai server proxy)
            responseHeaders.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());

            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);

        } catch (HttpStatusCodeException e) {
            log.warn("Loi HTTP {} tu CDN goc khi tai anh {}: {}. Tra ve fallback placeholder.", e.getStatusCode(), imageUrl, e.getMessage());
            return createFallbackResponse(HttpStatus.OK);
        } catch (Exception e) {
            log.warn("Loi khong mong muon khi proxy anh {}: {}. Tra ve fallback placeholder.", imageUrl, e.getMessage());
            return createFallbackResponse(HttpStatus.OK);
        }
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
