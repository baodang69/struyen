package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findBySlug(String slug);
    Optional<Category> findBySlugIgnoreCase(String slug);
    Optional<Category> findByNameIgnoreCase(String name);
    Optional<Category> findByNameViIgnoreCase(String nameVi);

    List<Category> findAllByOrderByNameAsc();

    @Query("SELECT c, COUNT(DISTINCT s) FROM Category c " +
           "LEFT JOIN c.stories s ON s.isActive = true " +
           "GROUP BY c.id, c.name, c.nameVi, c.slug " +
           "ORDER BY COALESCE(c.nameVi, c.name) ASC")
    List<Object[]> findAllCategoriesWithStoryCount();
}
