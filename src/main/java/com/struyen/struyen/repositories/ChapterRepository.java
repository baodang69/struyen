package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, String> {
    Optional<Chapter> findByStoryIdAndChapterName(String storyId, String chapterName);
}
