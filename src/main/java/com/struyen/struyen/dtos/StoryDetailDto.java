package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Story;
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
public class StoryDetailDto {
    private String id;
    private String name;
    private String slug;
    private String thumbUrl;
    private String cdnDomain;
    private String status;
    private String author;
    private String description;
    private Long viewCount;
    private Boolean isFeatured;
    private LocalDateTime updatedAt;
    private List<CategoryDto> categories;
    private List<ChapterShortDto> chapters;

    public static final String DEFAULT_CDN_IMAGE_DOMAIN = "https://img.otruyenapi.com/uploads/comics";

    public static StoryDetailDto fromEntity(Story story, List<ChapterShortDto> chapters) {
        if (story == null) return null;

        List<CategoryDto> categoryDtos = story.getCategories() != null
                ? story.getCategories().stream().map(CategoryDto::fromEntity).toList()
                : Collections.emptyList();

        String cdn = (story.getCdnDomain() != null && !story.getCdnDomain().isBlank())
                ? story.getCdnDomain()
                : DEFAULT_CDN_IMAGE_DOMAIN;

        return StoryDetailDto.builder()
                .id(story.getId())
                .name(story.getName())
                .slug(story.getSlug())
                .thumbUrl(story.getThumbUrl())
                .cdnDomain(cdn)
                .status(story.getStatus())
                .author(story.getAuthor())
                .description(story.getDescription())
                .viewCount(story.getViewCount())
                .isFeatured(story.getIsFeatured())
                .updatedAt(story.getUpdatedAt())
                .categories(categoryDtos)
                .chapters(chapters != null ? chapters : Collections.emptyList())
                .build();
    }
}

