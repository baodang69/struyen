package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Chapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterShortDto {
    private String id;
    private String chapterName;
    private String chapterTitle;
    private LocalDateTime updatedAt;

    public static ChapterShortDto fromEntity(Chapter chapter) {
        if (chapter == null) return null;
        return ChapterShortDto.builder()
                .id(chapter.getId())
                .chapterName(chapter.getChapterName())
                .chapterTitle(chapter.getChapterTitle())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}

