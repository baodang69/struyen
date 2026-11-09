package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Comment;
import com.struyen.struyen.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String storyId;
    private String chapterId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Long replyCount;
    private LocalDateTime createdAt;
    private List<CommentDto> replies;

    public static CommentDto fromEntity(Comment comment, long replyCount) {
        if (comment == null) return null;

        User user = comment.getUser();

        return CommentDto.builder()
                .id(comment.getId())
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : "Nguoi dung vo danh")
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .storyId(comment.getStory() != null ? comment.getStory().getId() : null)
                .chapterId(comment.getChapter() != null ? comment.getChapter().getId() : null)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(Boolean.TRUE.equals(comment.getIsDeleted()) ? "Binh luan nay da bi xoa." : comment.getContent())
                .likeCount(comment.getLikeCount())
                .replyCount(replyCount)
                .createdAt(comment.getCreatedAt())
                .replies(Collections.emptyList())
                .build();
    }
}
