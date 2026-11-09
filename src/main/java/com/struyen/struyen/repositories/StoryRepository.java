package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, String> {
    Optional<Story> findBySlug(String slug);

    @EntityGraph(attributePaths = {"categories"})
    Optional<Story> findDetailBySlug(String slug);

    List<Story> findBySlugIn(List<String> slugs);

    @EntityGraph(attributePaths = {"categories"})
    Page<Story> findByIsActiveTrue(Pageable pageable);
    
    @EntityGraph(attributePaths = {"categories"})
    Page<Story> findByIsActiveTrueAndCategoriesSlug(String categorySlug, Pageable pageable);

    @EntityGraph(attributePaths = {"categories"})
    Page<Story> findByIsActiveTrueAndCategoriesId(String categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"categories"})
    @Query(value = "SELECT DISTINCT s FROM Story s JOIN s.categories c " +
                   "WHERE s.isActive = true AND (" +
                   "LOWER(c.slug) = LOWER(:category) OR " +
                   "LOWER(c.name) = LOWER(:category)" +
                   ")",
           countQuery = "SELECT COUNT(DISTINCT s) FROM Story s JOIN s.categories c " +
                        "WHERE s.isActive = true AND (" +
                        "LOWER(c.slug) = LOWER(:category) OR " +
                        "LOWER(c.name) = LOWER(:category)" +
                        ")")
    Page<Story> findByIsActiveTrueAndCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT MAX(s.originUpdatedAt) FROM Story s")
    LocalDateTime findMaxOriginUpdatedAt();

    Page<Story> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String name, String slug, Pageable pageable);

    Page<Story> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
