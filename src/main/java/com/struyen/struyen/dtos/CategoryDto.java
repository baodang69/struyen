package com.struyen.struyen.dtos;

import com.struyen.struyen.entities.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private String id;
    private String name;
    private String nameVi;
    private String slug;
    private Long storyCount;

    public static CategoryDto fromEntity(Category category) {
        return fromEntity(category, null);
    }

    public static CategoryDto fromEntity(Category category, Long storyCount) {
        if (category == null) return null;
        String displayVi = (category.getNameVi() != null && !category.getNameVi().isBlank())
                ? category.getNameVi()
                : category.getName();

        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .nameVi(displayVi)
                .slug(category.getSlug())
                .storyCount(storyCount)
                .build();
    }
}

