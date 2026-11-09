package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.StorySubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorySubscriptionRepository extends JpaRepository<StorySubscription, Long> {

    boolean existsByUserIdAndStoryId(Long userId, String storyId);

    Optional<StorySubscription> findByUserIdAndStoryId(Long userId, String storyId);

    @EntityGraph(attributePaths = {"story"})
    Page<StorySubscription> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    void deleteByUserIdAndStoryId(Long userId, String storyId);

    long countByStoryId(String storyId);
}
