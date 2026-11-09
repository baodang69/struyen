package com.struyen.struyen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProxyService {

    private final RestTemplate restTemplate;

    // Giới hạn dung lượng tối đa cho một file ảnh (15 MB) để chống tràn bộ nhớ Heap (OOM)
    private static final long MAX_IMAGE_SIZE_BYTES = 15 * 1024 * 1024;

    // Whitelist các domain CDN hợp lệ của Otruyen để ngăn chặn hoàn toàn SSRF
    private static final Set<String> ALLOWED_HOST_SUFFIXES = Set.of(
            "otruyencdn.com",
            "otruyenapi.com"
    );

    public ResponseEntity<byte[]> fetchImage(String imageUrl) {
        // 1. Kiem tra an toan bao mat SSRF (Validate Domain Whitelist & Protocol)
        if (!isAllowedUrl(imageUrl)) {
            log.warn("[SSRF Blocked] Tu choi truy cap URL khong thuoc whitelist: {}", imageUrl);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://otruyenapi.com/");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            // Kiểm tra Content-Length để bảo vệ Heap RAM khỏi OOM
            List<String> contentLengthHeaders = response.getHeaders().get(HttpHeaders.CONTENT_LENGTH);
            if (contentLengthHeaders != null && !contentLengthHeaders.isEmpty()) {
                long contentLength = Long.parseLong(contentLengthHeaders.get(0));
                if (contentLength > MAX_IMAGE_SIZE_BYTES) {
                    log.warn("[OOM Protection] Tu choi anh co dung luong vuot nguong: {} bytes", contentLength);
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(null);
                }
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null) {
                responseHeaders.setContentType(contentType);
            } else {
                responseHeaders.setContentType(MediaType.IMAGE_JPEG);
            }

            // Thiết lập Cache-Control để trình duyệt tự do cache trong 30 ngày (giúp giảm tải server proxy)
            responseHeaders.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());

            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);

        } catch (HttpStatusCodeException e) {
            log.error("Lỗi HTTP {} từ CDN gốc khi tải ảnh {}: {}", e.getStatusCode(), imageUrl, e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(null);
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi proxy ảnh {}: {}", imageUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
        }
    }

    /**
     * Xác thực URL nhằm chống tấn công SSRF (Server-Side Request Forgery).
     * Chỉ chấp nhận giao thức http/https và hostname thuộc danh sách domain của Otruyen CDN.
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

            // Chặn tuyệt đối localhost, loopback và private network
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1") ||
                host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("169.254.") || host.startsWith("172.")) {
                return false;
            }

            // Kiểm tra xem host có thuộc whitelist không
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
