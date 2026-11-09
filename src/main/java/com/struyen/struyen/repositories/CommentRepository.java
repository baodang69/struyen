package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findByStoryIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(String storyId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findByChapterIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(String chapterId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);

    long countByParentIdAndIsDeletedFalse(Long parentId);

    long countByStoryIdAndIsDeletedFalse(String storyId);

    long countByChapterIdAndIsDeletedFalse(String chapterId);
}

