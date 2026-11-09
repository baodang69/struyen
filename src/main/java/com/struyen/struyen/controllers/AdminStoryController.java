package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.AdminStoryDto;
import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoryController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminStoryDto>> getStories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(adminService.getAdminStories(page, size, keyword));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable String id) {
        boolean newStatus = adminService.toggleStoryActive(id);
        return ResponseEntity.ok(Map.of("storyId", id, "isActive", newStatus));
    }

    @PatchMapping("/{id}/toggle-featured")
    public ResponseEntity<Map<String, Object>> toggleFeatured(@PathVariable String id) {
        boolean newStatus = adminService.toggleStoryFeatured(id);
        return ResponseEntity.ok(Map.of("storyId", id, "isFeatured", newStatus));
    }
}
