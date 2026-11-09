package com.struyen.struyen.services;

import com.struyen.struyen.dtos.PageResponse;
import com.struyen.struyen.dtos.StoryListDto;
import com.struyen.struyen.entities.Story;
import com.struyen.struyen.repositories.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicStoryService {
    
    private final StoryRepository storyRepository;
    
    public PageResponse<StoryListDto> getStories(int page, int size, String categorySlug) {
        // Handle 1-indexed requests from clients (Spring uses 0-indexed)
        int springPage = page > 0 ? page - 1 : 0;
        
        // Sort by updatedAt descending by default for latest updates
        Pageable pageable = PageRequest.of(springPage, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        Page<Story> storyPage;
        if (categorySlug != null && !categorySlug.isEmpty()) {
            storyPage = storyRepository.findByIsActiveTrueAndCategoriesSlug(categorySlug, pageable);
        } else {
            storyPage = storyRepository.findByIsActiveTrue(pageable);
        }
        
        Page<StoryListDto> dtoPage = storyPage.map(StoryListDto::fromEntity);
        
        return PageResponse.of(dtoPage);
    }
}
