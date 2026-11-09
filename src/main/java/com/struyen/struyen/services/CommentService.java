package com.struyen.struyen.services;

import com.struyen.struyen.dtos.CommentDto;
import com.struyen.struyen.dtos.CreateCommentRequest;
import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.entities.Chapter;
import com.struyen.struyen.entities.Comment;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.entities.User;
import com.struyen.struyen.repositories.ChapterRepository;
import com.struyen.struyen.repositories.CommentRepository;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    /**
     * Tao binh luan moi (hoac reply mot binh luan khac)
     */
    @Transactional
    public CommentDto createComment(String username, CreateCommentRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay user: " + username));

        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay truyen: " + request.getStoryId()));

        Chapter chapter = null;
        if (request.getChapterId() != null && !request.getChapterId().isBlank()) {
            chapter = chapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay chuong: " + request.getChapterId()));
        }

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay binh luan goc can reply: " + request.getParentId()));

            // Dam bao reply chi o 2 cap (neu parent la mot reply thi gan ve root comment)
            if (parent.getParent() != null) {
                parent = parent.getParent();
            }
        }

        Comment comment = Comment.builder()
                .user(user)
                .story(story)
                .chapter(chapter)
                .parent(parent)
                .content(request.getContent().trim())
                .likeCount(0)
                .isDeleted(false)
                .build();

        comment = commentRepository.save(comment);
        log.info("User {} da tao binh luan tren truyen {}", username, story.getId());
        return CommentDto.fromEntity(comment, 0);
    }

    /**
     * Lay danh sach binh luan cap goc tren trang truyen (Story Page)
     */
    @Transactional(readOnly = true)
    public PageResponse<CommentDto> getStoryComments(String storyId, int page, int size) {
        int springPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(springPage, size);

        Page<Comment> commentPage = commentRepository.findByStoryIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(storyId, pageable);

        Page<CommentDto> dtoPage = commentPage.map(c -> {
            long replyCount = commentRepository.countByParentIdAndIsDeletedFalse(c.getId());
            return CommentDto.fromEntity(c, replyCount);
        });

        return PageResponse.of(dtoPage);
    }

    /**
     * Lay danh sach binh luan cap goc trong chuong truyen (Chapter Page)
     */
    @Transactional(readOnly = true)
    public PageResponse<CommentDto> getChapterComments(String chapterId, int page, int size) {
        int springPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(springPage, size);

        Page<Comment> commentPage = commentRepository.findByChapterIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(chapterId, pageable);

        Page<CommentDto> dtoPage = commentPage.map(c -> {
            long replyCount = commentRepository.countByParentIdAndIsDeletedFalse(c.getId());
            return CommentDto.fromEntity(c, replyCount);
        });

        return PageResponse.of(dtoPage);
    }

    /**
     * Lay toan bo reply cua mot binh luan goc
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getReplies(Long parentId) {
        List<Comment> replies = commentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentId);
        return replies.stream()
                .map(r -> CommentDto.fromEntity(r, 0))
                .toList();
    }

    /**
     * Tang luot like cua binh luan
     */
    @Transactional
    public int likeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay binh luan: " + commentId));

        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
        return comment.getLikeCount();
    }

    /**
     * Xoa mem binh luan (chi chu binh luan hoac Admin moi duoc xoa)
     */
    @Transactional
    public void deleteComment(String username, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay binh luan: " + commentId));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay user: " + username));

        boolean isOwner = comment.getUser() != null && comment.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xoa binh luan nay.");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
        log.info("Binh luan {} da duoc xoa mem boi user {}", commentId, username);
    }
}
