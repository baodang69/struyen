package com.struyen.struyen.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class ImageProxyService {

    private final RestTemplate restTemplate;

    @Autowired
    public ImageProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<byte[]> fetchImage(String imageUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            // Gỉa mạo Referer và User-Agent để vượt qua Hotlink Protection
            headers.set("Referer", "https://otruyenapi.com/");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            // Giữ lại Content-Type của hình ảnh gốc
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

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
