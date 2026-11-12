package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, String>, JpaSpecificationExecutor<Story> {
    Optional<Story> findBySlug(String slug);

    @EntityGraph(attributePaths = {"categories"})
    Optional<Story> findDetailBySlug(String slug);

    List<Story> findBySlugIn(List<String> slugs);

    Page<Story> findByIsActiveTrue(Pageable pageable);
    
    Page<Story> findByIsActiveTrueAndCategoriesSlug(String categorySlug, Pageable pageable);

    Page<Story> findByIsActiveTrueAndCategoriesId(String categoryId, Pageable pageable);

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

    @Query("SELECT s FROM Story s WHERE s.isActive = true AND s.isFeatured = true ORDER BY s.featuredAt DESC NULLS LAST, s.updatedAt DESC")
    List<Story> findFeaturedStories(Pageable pageable);

    @EntityGraph(attributePaths = {"categories"})
    @Query("SELECT s FROM Story s WHERE s.isFeatured = true ORDER BY s.featuredAt DESC NULLS LAST, s.updatedAt DESC")
    List<Story> findAllFeaturedStories();

    List<Story> findByIsActiveTrueOrderByViewCountDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE story s SET latest_chapter_name = (" +
                   "  SELECT c.chapter_name FROM chapter c WHERE c.story_id = s.id ORDER BY c.created_at DESC LIMIT 1" +
                   ") WHERE s.latest_chapter_name IS NULL", nativeQuery = true)
    int backfillLatestChapterNames();

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE story s SET featured_at = s.updated_at WHERE s.is_featured = true AND s.featured_at IS NULL", nativeQuery = true)
    int backfillFeaturedAt();
}
