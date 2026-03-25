package com.struyen.struyen.controllers;

import com.struyen.struyen.services.OtruyenSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
public class AdminSyncController {

    private final OtruyenSyncService otruyenSyncService;

    @PostMapping("/full")
    public ResponseEntity<String> triggerFullSync(
            @RequestParam(defaultValue = "1") int startPage,
            @RequestParam(defaultValue = "10") int endPage) {

        // Gọi hàm Async để chạy ngầm tiến trình kéo data, không làm treo Web Request (Timeout)
        otruyenSyncService.syncFullStoriesAsync(startPage, endPage);

        return ResponseEntity.ok("Tiến trình cào Full Data từ trang " + startPage + " đến trang " + endPage + " đã được khởi chạy ngầm (Background Task). Vui lòng theo dõi tiến độ trên màn hình Console Logs nhé!");
    }
}
