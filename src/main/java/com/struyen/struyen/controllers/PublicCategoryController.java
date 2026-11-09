package com.struyen.struyen.controllers;

import com.struyen.struyen.dtos.CategoryDto;
import com.struyen.struyen.services.PublicStoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final PublicStoryService publicStoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        return ResponseEntity.ok(publicStoryService.getAllCategories());
    }
}

