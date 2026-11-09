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
public class StoryListDto {
    private String id;
    private String name;
    private String slug;
    private String thumbUrl;
    private String cdnDomain;
    private String status;
    private Long viewCount;
    private Boolean isFeatured;
    private LocalDateTime updatedAt;
    
    public static StoryListDto fromEntity(Story story) {
        return StoryListDto.builder()
                .id(story.getId())
                .name(story.getName())
                .slug(story.getSlug())
                .thumbUrl(story.getThumbUrl())
                .cdnDomain(story.getCdnDomain())
                .status(story.getStatus())
                .viewCount(story.getViewCount())
                .isFeatured(story.getIsFeatured())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
}
