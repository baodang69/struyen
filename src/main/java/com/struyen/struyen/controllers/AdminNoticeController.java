package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.CreateNoticeRequest;
import com.struyen.struyen.dtos.NoticeDto;
import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.services.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNoticeController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<PageResponse<NoticeDto>> getAllNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllNotices(page, size));
    }

    @PostMapping
    public ResponseEntity<NoticeDto> createNotice(@Valid @RequestBody CreateNoticeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createNotice(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotice(@PathVariable Long id) {
        adminService.deleteNotice(id);
        return ResponseEntity.ok(Map.of("message", "Da xoa thong bao he thong thanh cong."));
    }
}
