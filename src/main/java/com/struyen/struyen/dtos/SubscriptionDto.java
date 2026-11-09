package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Story;
import com.struyen.struyen.entities.StorySubscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private Long id;
    private String storyId;
    private String storyName;
    private String storySlug;
    private String thumbUrl;
    private String status;
    private LocalDateTime subscribedAt;

    public static SubscriptionDto fromEntity(StorySubscription sub) {
        if (sub == null) return null;
        Story story = sub.getStory();
        return SubscriptionDto.builder()
                .id(sub.getId())
                .storyId(story != null ? story.getId() : null)
                .storyName(story != null ? story.getName() : null)
                .storySlug(story != null ? story.getSlug() : null)
                .thumbUrl(story != null ? story.getThumbUrl() : null)
                .status(story != null ? story.getStatus() : null)
                .subscribedAt(sub.getCreatedAt())
                .build();
    }
}
