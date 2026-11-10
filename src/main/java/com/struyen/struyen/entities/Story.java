package com.struyen.struyen.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "story", indexes = {
    @Index(name = "idx_story_slug", columnList = "slug"),
    @Index(name = "idx_story_updated_at", columnList = "updated_at DESC"),
    @Index(name = "idx_story_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Story {
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    @Column(nullable = false)
    private String name;
    
    private String status;
    private String thumbUrl;
    private String cdnDomain;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String author;
    
    @Builder.Default
    private Long viewCount = 0L;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private Boolean isFeatured = false;

    @Builder.Default
    private String language = "vi";

    @Builder.Default
    private Boolean hasVietnameseTranslation = true;
    
    private LocalDateTime originUpdatedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastChapterSyncAt;
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "story_category",
        joinColumns = @JoinColumn(name = "story_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
    
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Chapter> chapters = new HashSet<>();
}
