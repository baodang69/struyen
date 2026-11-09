package com.struyen.struyen.controllers;

import com.struyen.struyen.services.OtruyenSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
@Slf4j
public class AdminSyncController {

    private final OtruyenSyncService otruyenSyncService;

    @Value("${admin.secret-key:admin-struyen-secret-key-2026}")
    private String adminSecretKey;

    @PostMapping("/full")
    public ResponseEntity<String> triggerFullSync(
            @RequestHeader(value = "X-Admin-Key", required = false) String providedKey,
            @RequestParam(defaultValue = "1") int startPage,
            @RequestParam(defaultValue = "10") int endPage) {

        // Bao ve API Admin Sync: Yeu cau Header X-Admin-Key hop le
        if (providedKey == null || !providedKey.equals(adminSecretKey)) {
            log.warn("[Unauthorized Admin Access] Truy cập trái phép endpoint cào dữ liệu Admin!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Header 'X-Admin-Key' không hợp lệ hoặc bị thiếu.");
        }

        // Giới hạn an toàn khoảng cách trang cào
        if (startPage < 1 || endPage < startPage || (endPage - startPage) > 100) {
            return ResponseEntity.badRequest()
                    .body("Tham số không hợp lệ: startPage phải >= 1, endPage >= startPage và khoảng cách quét tối đa 100 trang.");
        }

        // Gọi hàm Async để chạy ngầm tiến trình kéo data, không làm treo Web Request (Timeout)
        otruyenSyncService.syncFullStoriesAsync(startPage, endPage);

        return ResponseEntity.ok("Tiến trình cào Full Data từ trang " + startPage + " đến trang " + endPage + " đã được khởi chạy ngầm (Background Task). Vui lòng theo dõi tiến độ trên màn hình Console Logs nhé!");
    }
}
