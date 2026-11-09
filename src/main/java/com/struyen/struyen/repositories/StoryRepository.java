package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, String> {
    Optional<Story> findBySlug(String slug);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"categories"})
    Optional<Story> findDetailBySlug(String slug);

    List<Story> findBySlugIn(List<String> slugs);

    Page<Story> findByIsActiveTrue(Pageable pageable);
    
    Page<Story> findByIsActiveTrueAndCategoriesSlug(String categorySlug, Pageable pageable);

    @Query("SELECT MAX(s.originUpdatedAt) FROM Story s")
    LocalDateTime findMaxOriginUpdatedAt();

    Page<Story> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String name, String slug, Pageable pageable);

    Page<Story> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
