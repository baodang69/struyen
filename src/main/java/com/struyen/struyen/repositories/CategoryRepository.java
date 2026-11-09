package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findBySlug(String slug);
    Optional<Category> findBySlugIgnoreCase(String slug);
    Optional<Category> findByNameIgnoreCase(String name);

    java.util.List<Category> findAllByOrderByNameAsc();
}
