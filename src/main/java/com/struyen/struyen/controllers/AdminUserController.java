package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.AdminUserDto;
import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminUserDto>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.getAdminUsers(page, size, status));
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<Map<String, String>> banUser(@PathVariable Long id) {
        adminService.banUser(id);
        return ResponseEntity.ok(Map.of("message", "Da khoa tai khoan nguoi dung thanh cong."));
    }

    @PatchMapping("/{id}/unban")
    public ResponseEntity<Map<String, String>> unbanUser(@PathVariable Long id) {
        adminService.unbanUser(id);
        return ResponseEntity.ok(Map.of("message", "Da mo khoa tai khoan nguoi dung thanh cong."));
    }
}

