package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.dtos.StoryListDto;
import com.struyen.struyen.services.PublicStoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/stories")
@RequiredArgsConstructor
@CrossOrigin("*") // Allow frontend requests
public class PublicStoryController {

    private final PublicStoryService publicStoryService;

    @GetMapping
    public ResponseEntity<PageResponse<StoryListDto>> getStories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category) {

        return ResponseEntity.ok(publicStoryService.getStories(page, size, category));
    }
}
