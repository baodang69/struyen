package com.struyen.struyen.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "chapter", indexes = {
    @Index(name = "idx_chapter_story", columnList = "story_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"story_id", "chapter_name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chapter {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;
    
    @Column(nullable = false)
    private String chapterName;
    
    private String chapterTitle;
    
    @Column(nullable = false, unique = true)
    private String chapterApiData;
    
    // Spring Boot 3 / Hibernate 6 native JSON mapping
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> imagesJson;
    
    @Builder.Default
    private Boolean isContentSynced = false;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
