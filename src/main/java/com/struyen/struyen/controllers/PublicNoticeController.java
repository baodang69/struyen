package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.NoticeDto;
import com.struyen.struyen.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/notices")
@RequiredArgsConstructor
public class PublicNoticeController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<NoticeDto>> getActiveNotices() {
        return ResponseEntity.ok(adminService.getActiveNotices());
    }
}
