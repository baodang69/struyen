package com.struyen.struyen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "category", indexes = {
    @Index(name = "idx_category_slug", columnList = "slug"),
    @Index(name = "idx_category_name_vi", columnList = "name_vi")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;

    private String nameVi;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    @ManyToMany(mappedBy = "categories")
    @Builder.Default
    private Set<Story> stories = new HashSet<>();
}
