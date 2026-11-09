package com.struyen.struyen.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDetailDto {
    private String chapterId;
    private String storyId;
    private String storySlug;
    private String storyName;
    private String chapterName;
    private String chapterTitle;
    private String prevChapterId;
    private String nextChapterId;
    private List<String> images;
}

