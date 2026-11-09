package com.struyen.struyen.services;

import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.dtos.ReadingHistoryDto;
import com.struyen.struyen.dtos.RecordHistoryRequest;
import com.struyen.struyen.dtos.SubscriptionDto;
import com.struyen.struyen.entities.Chapter;
import com.struyen.struyen.entities.ReadingHistory;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.entities.StorySubscription;
import com.struyen.struyen.entities.User;
import com.struyen.struyen.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLibraryService {

    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StorySubscriptionRepository subscriptionRepository;
    private final ReadingHistoryRepository historyRepository;

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay user: " + username));
    }

    private Story getStoryById(String storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay truyen: " + storyId));
    }

    /**
     * Theo doi truyen (Subscription)
     */
    @Transactional
    public void subscribe(String username, String storyId) {
        User user = getUserByUsername(username);
        Story story = getStoryById(storyId);

        if (!subscriptionRepository.existsByUserIdAndStoryId(user.getId(), story.getId())) {
            StorySubscription subscription = StorySubscription.builder()
                    .user(user)
                    .story(story)
                    .build();
            subscriptionRepository.save(subscription);
            log.info("User {} da theo doi truyen {}", username, story.getName());
        }
    }

    /**
     * Bo theo doi truyen
     */
    @Transactional
    public void unsubscribe(String username, String storyId) {
        User user = getUserByUsername(username);
        subscriptionRepository.deleteByUserIdAndStoryId(user.getId(), storyId);
        log.info("User {} da huy theo doi truyen {}", username, storyId);
    }

    /**
     * Kiem tra trang thai theo doi truyen
     */
    @Transactional(readOnly = true)
    public boolean isSubscribed(String username, String storyId) {
        User user = getUserByUsername(username);
        return subscriptionRepository.existsByUserIdAndStoryId(user.getId(), storyId);
    }

    /**
     * Lay danh sach truyen dang theo doi co phan trang
     */
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionDto> getSubscriptions(String username, int page, int size) {
        User user = getUserByUsername(username);
        int springPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(springPage, size);

        Page<StorySubscription> subPage = subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return PageResponse.of(subPage.map(SubscriptionDto::fromEntity));
    }

    /**
     * Ghi nhan hoac cap nhat lich su doc truyen
     */
    @Transactional
    public ReadingHistoryDto recordReadingHistory(String username, RecordHistoryRequest request) {
        User user = getUserByUsername(username);
        Story story = getStoryById(request.getStoryId());
        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay chapter: " + request.getChapterId()));

        ReadingHistory history = historyRepository.findByUserIdAndStoryId(user.getId(), story.getId())
                .orElseGet(() -> ReadingHistory.builder()
                        .user(user)
                        .story(story)
                        .build());

        history.setLastChapter(chapter);
        history.setReadingAt(LocalDateTime.now());

        history = historyRepository.save(history);
        return ReadingHistoryDto.fromEntity(history);
    }

    /**
     * Lay danh sach lich su doc co phan trang
     */
    @Transactional(readOnly = true)
    public PageResponse<ReadingHistoryDto> getReadingHistory(String username, int page, int size) {
        User user = getUserByUsername(username);
        int springPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(springPage, size);

        Page<ReadingHistory> historyPage = historyRepository.findByUserIdOrderByReadingAtDesc(user.getId(), pageable);
        return PageResponse.of(historyPage.map(ReadingHistoryDto::fromEntity));
    }

    /**
     * Xoa 1 muc khoi lich su doc
     */
    @Transactional
    public void deleteReadingHistoryItem(String username, String storyId) {
        User user = getUserByUsername(username);
        historyRepository.deleteByUserIdAndStoryId(user.getId(), storyId);
    }
}

