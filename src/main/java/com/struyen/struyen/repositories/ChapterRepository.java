package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, String> {
    Optional<Chapter> findByStoryIdAndChapterName(String storyId, String chapterName);

    /**
     * Lấy toàn bộ tên chapter đã tồn tại của một truyện trong 1 câu query duy nhất.
     * Giải quyết triệt để vấn đề N+1 queries khi kiểm tra từng chapter trong vòng lặp.
     */
    @Query("SELECT c.chapterName FROM Chapter c WHERE c.story.id = :storyId")
    Set<String> findChapterNamesByStoryId(@Param("storyId") String storyId);

    /**
     * Lấy toàn bộ danh sách chương của một truyện theo thứ tự tạo tăng dần.
     */
    java.util.List<Chapter> findByStoryIdOrderByCreatedAtAsc(String storyId);

}
