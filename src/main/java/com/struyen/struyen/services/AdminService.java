package com.struyen.struyen.services;

import com.struyen.struyen.dtos.*;
import com.struyen.struyen.entities.Notice;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.entities.User;
import com.struyen.struyen.repositories.NoticeRepository;
import com.struyen.struyen.repositories.StoryRepository;
import com.struyen.struyen.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final NoticeRepository noticeRepository;

    // ==========================================
    // 1. STORY MODERATION
    // ==========================================

    @Transactional
    public boolean toggleStoryActive(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay truyen: " + storyId));

        boolean newStatus = !Boolean.TRUE.equals(story.getIsActive());
        story.setIsActive(newStatus);
        storyRepository.save(story);
        log.info("Admin da doi trang thai isActive cua truyen {} thanh: {}", story.getName(), newStatus);
        return newStatus;
    }

    @Transactional
    public boolean toggleStoryFeatured(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay truyen: " + storyId));

        boolean newFeatured = !Boolean.TRUE.equals(story.getIsFeatured());
        story.setIsFeatured(newFeatured);
        storyRepository.save(story);
        log.info("Admin da doi trang thai isFeatured cua truyen {} thanh: {}", story.getName(), newFeatured);
        return newFeatured;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminStoryDto> getAdminStories(int page, int size, String keyword) {
        int springPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(springPage, size);

        Page<Story> storyPage;
        if (keyword != null && !keyword.isBlank()) {
            storyPage = storyRepository.findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(keyword.trim(), keyword.trim(), pageable);
        } else {
            storyPage = storyRepository.findAllByOrderByUpdatedAtDesc(pageable);
        }

        return PageResponse.of(storyPage.map(AdminStoryDto::fromEntity));
    }

    // ==========================================
    // 2. USER MODERATION
    // ==========================================

    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay user: " + userId));

        user.setUserStatus("BANNED");
        userRepository.save(user);
        log.info("Admin da khoa tai khoan user: {}", user.getUsername());
    }

    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay user: " + userId));

        user.setUserStatus("ACTIVE");
        userRepository.save(user);
        log.info("Admin da mo khoa tai khoan user: {}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserDto> getAdminUsers(int page, int size, String status) {
        int springPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(springPage, size);

        Page<User> userPage;
        if (status != null && !status.isBlank()) {
            userPage = userRepository.findByUserStatusOrderByCreatedAtDesc(status.trim().toUpperCase(), pageable);
        } else {
            userPage = userRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return PageResponse.of(userPage.map(AdminUserDto::fromEntity));
    }

    // ==========================================
    // 3. SYSTEM NOTICES
    // ==========================================

    @Transactional
    public NoticeDto createNotice(CreateNoticeRequest request) {
        Notice notice = Notice.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        notice = noticeRepository.save(notice);
        log.info("Admin da tao thong bao he thong moi: {}", notice.getTitle());
        return NoticeDto.fromEntity(notice);
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeDto> getAllNotices(int page, int size) {
        int springPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(springPage, size);

        Page<Notice> noticePage = noticeRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.of(noticePage.map(NoticeDto::fromEntity));
    }

    @Transactional(readOnly = true)
    public List<NoticeDto> getActiveNotices() {
        return noticeRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(NoticeDto::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay thong bao: " + noticeId);
        }
        noticeRepository.deleteById(noticeId);
        log.info("Admin da xoa thong bao he thong: {}", noticeId);
    }
}
