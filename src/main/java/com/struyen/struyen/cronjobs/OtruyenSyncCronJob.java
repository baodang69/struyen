package com.struyen.struyen.cronjobs;

import com.struyen.struyen.services.OtruyenSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sync.otruyen.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class OtruyenSyncCronJob {

    private final OtruyenSyncService otruyenSyncService;

    // Chạy mặc định mỗi 30 phút (1800000 mili giây)
    @Scheduled(fixedDelayString = "${sync.otruyen.fixed-delay:1800000}")
    public void syncNewStoriesJob() {
        log.info("--- CRON JOB BẮT ĐẦU: Cập nhật truyện mới từ Otruyen ---");
        try {
            // Sử dụng cơ chế nội suy Catch-up an toàn chống mất file khi sập mạng
            otruyenSyncService.syncCatchUpStories();
        } catch (Exception e) {
            log.error("Lỗi khi chạy Cron Job sync truyện: {}", e.getMessage(), e);
        }
        log.info("--- CRON JOB HOÀN TẤT: Cập nhật truyện mới ---");
    }
}
