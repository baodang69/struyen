package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.ReadingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {

    Optional<ReadingHistory> findByUserIdAndStoryId(Long userId, String storyId);

    @EntityGraph(attributePaths = {"story", "lastChapter"})
    Page<ReadingHistory> findByUserIdOrderByReadingAtDesc(Long userId, Pageable pageable);

    void deleteByUserIdAndStoryId(Long userId, String storyId);
}

