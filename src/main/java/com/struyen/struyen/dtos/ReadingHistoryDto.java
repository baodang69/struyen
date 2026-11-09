package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Chapter;
import com.struyen.struyen.entities.ReadingHistory;
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
public class ReadingHistoryDto {
    private Long id;
    private String storyId;
    private String storyName;
    private String storySlug;
    private String thumbUrl;
    private String lastChapterId;
    private String lastChapterName;
    private String lastChapterTitle;
    private LocalDateTime readingAt;

    public static ReadingHistoryDto fromEntity(ReadingHistory history) {
        if (history == null) return null;
        Story story = history.getStory();
        Chapter chapter = history.getLastChapter();

        return ReadingHistoryDto.builder()
                .id(history.getId())
                .storyId(story != null ? story.getId() : null)
                .storyName(story != null ? story.getName() : null)
                .storySlug(story != null ? story.getSlug() : null)
                .thumbUrl(story != null ? story.getThumbUrl() : null)
                .lastChapterId(chapter != null ? chapter.getId() : null)
                .lastChapterName(chapter != null ? chapter.getChapterName() : null)
                .lastChapterTitle(chapter != null ? chapter.getChapterTitle() : null)
                .readingAt(history.getReadingAt())
                .build();
    }
}

