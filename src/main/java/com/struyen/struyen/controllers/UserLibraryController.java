package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.dtos.ReadingHistoryDto;
import com.struyen.struyen.dtos.RecordHistoryRequest;
import com.struyen.struyen.dtos.SubscriptionDto;
import com.struyen.struyen.services.UserLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me/library")
@RequiredArgsConstructor
public class UserLibraryController {

    private final UserLibraryService userLibraryService;

    @PostMapping("/subscriptions/{storyId}")
    public ResponseEntity<Map<String, String>> subscribe(
            Authentication authentication,
            @PathVariable String storyId) {
        userLibraryService.subscribe(authentication.getName(), storyId);
        return ResponseEntity.ok(Map.of("message", "Da theo doi truyen thanh cong."));
    }

    @DeleteMapping("/subscriptions/{storyId}")
    public ResponseEntity<Map<String, String>> unsubscribe(
            Authentication authentication,
            @PathVariable String storyId) {
        userLibraryService.unsubscribe(authentication.getName(), storyId);
        return ResponseEntity.ok(Map.of("message", "Da huy theo doi truyen."));
    }

    @GetMapping("/subscriptions/status/{storyId}")
    public ResponseEntity<Map<String, Boolean>> checkSubscriptionStatus(
            Authentication authentication,
            @PathVariable String storyId) {
        boolean isSubscribed = userLibraryService.isSubscribed(authentication.getName(), storyId);
        return ResponseEntity.ok(Map.of("isSubscribed", isSubscribed));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<PageResponse<SubscriptionDto>> getSubscriptions(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userLibraryService.getSubscriptions(authentication.getName(), page, size));
    }

    @PostMapping("/history")
    public ResponseEntity<ReadingHistoryDto> recordHistory(
            Authentication authentication,
            @Valid @RequestBody RecordHistoryRequest request) {
        return ResponseEntity.ok(userLibraryService.recordReadingHistory(authentication.getName(), request));
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<ReadingHistoryDto>> getHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userLibraryService.getReadingHistory(authentication.getName(), page, size));
    }

    @DeleteMapping("/history/{storyId}")
    public ResponseEntity<Map<String, String>> deleteHistoryItem(
            Authentication authentication,
            @PathVariable String storyId) {
        userLibraryService.deleteReadingHistoryItem(authentication.getName(), storyId);
        return ResponseEntity.ok(Map.of("message", "Da xoa muc khoi lich su doc."));
    }
}

