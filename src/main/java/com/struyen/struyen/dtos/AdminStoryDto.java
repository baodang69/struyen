package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Story;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStoryDto {
    private String id;
    private String name;
    private String slug;
    private String thumbUrl;
    private String status;
    private String author;
    private Boolean isActive;
    private Boolean isFeatured;
    private Long viewCount;
    private LocalDateTime updatedAt;

    public static AdminStoryDto fromEntity(Story story) {
        if (story == null) return null;
        return AdminStoryDto.builder()
                .id(story.getId())
                .name(story.getName())
                .slug(story.getSlug())
                .thumbUrl(story.getThumbUrl())
                .status(story.getStatus())
                .author(story.getAuthor())
                .isActive(story.getIsActive())
                .isFeatured(story.getIsFeatured())
                .viewCount(story.getViewCount())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
}

