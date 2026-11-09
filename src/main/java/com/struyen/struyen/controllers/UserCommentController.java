package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.CommentDto;
import com.struyen.struyen.dtos.CreateCommentRequest;
import com.struyen.struyen.services.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me/comments")
@RequiredArgsConstructor
public class UserCommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            Authentication authentication,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(authentication.getName(), request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(
            Authentication authentication,
            @PathVariable Long commentId) {
        commentService.deleteComment(authentication.getName(), commentId);
        return ResponseEntity.ok(Map.of("message", "Da xoa binh luan thanh cong."));
    }
}
