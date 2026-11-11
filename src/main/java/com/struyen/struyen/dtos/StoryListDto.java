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
public class StoryListDto {
    public static final String DEFAULT_CDN_IMAGE_DOMAIN = "https://img.otruyenapi.com/uploads/comics";

    private String id;
    private String name;
    private String slug;
    private String thumbUrl;
    private String cdnDomain;
    private String status;
    private Long viewCount;
    private Boolean isFeatured;
    private LocalDateTime featuredAt;
    private String latestChapterName;
    private LocalDateTime updatedAt;
    private String language;
    private Boolean hasVietnameseTranslation;
    private List<CategoryDto> categories;
    
    public static StoryListDto fromEntity(Story story) {
        String cdn = (story.getCdnDomain() != null && !story.getCdnDomain().isBlank())
                ? story.getCdnDomain()
                : DEFAULT_CDN_IMAGE_DOMAIN;

        List<CategoryDto> categoryDtos = (story.getCategories() != null)
                ? story.getCategories().stream().map(CategoryDto::fromEntity).toList()
                : Collections.emptyList();

        return StoryListDto.builder()
                .id(story.getId())
                .name(story.getName())
                .slug(story.getSlug())
                .thumbUrl(story.getThumbUrl())
                .cdnDomain(cdn)
                .status(story.getStatus())
                .viewCount(story.getViewCount())
                .isFeatured(story.getIsFeatured())
                .featuredAt(story.getFeaturedAt())
                .latestChapterName(story.getLatestChapterName())
                .updatedAt(story.getUpdatedAt())
                .language(story.getLanguage() != null ? story.getLanguage() : "vi")
                .hasVietnameseTranslation(story.getHasVietnameseTranslation() != null ? story.getHasVietnameseTranslation() : true)
                .categories(categoryDtos)
                .build();
    }
}
