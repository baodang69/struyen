package com.struyen.struyen.controllers;

import com.struyen.struyen.services.ImageProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/proxy")
public class ImageProxyController {

    private final ImageProxyService imageProxyService;

    @Autowired
    public ImageProxyController(ImageProxyService imageProxyService) {
        this.imageProxyService = imageProxyService;
    }

    @GetMapping("/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return imageProxyService.fetchImage(imageUrl);
    }
}
