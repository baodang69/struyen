package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.CommentDto;
import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/comments")
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping("/story/{storyId}")
    public ResponseEntity<PageResponse<CommentDto>> getStoryComments(
            @PathVariable String storyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commentService.getStoryComments(storyId, page, size));
    }

    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<PageResponse<CommentDto>> getChapterComments(
            @PathVariable String chapterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commentService.getChapterComments(chapterId, page, size));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentDto>> getReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<Map<String, Object>> likeComment(@PathVariable Long commentId) {
        int newLikes = commentService.likeComment(commentId);
        return ResponseEntity.ok(Map.of("commentId", commentId, "likeCount", newLikes));
    }
}
