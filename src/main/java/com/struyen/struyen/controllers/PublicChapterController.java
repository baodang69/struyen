package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.ChapterDetailDto;
import com.struyen.struyen.services.PublicStoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/chapters")
@RequiredArgsConstructor
public class PublicChapterController {

    private final PublicStoryService publicStoryService;

    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterDetailDto> getChapterDetail(@PathVariable String chapterId) {
        return ResponseEntity.ok(publicStoryService.getChapterDetail(chapterId));
    }
}

