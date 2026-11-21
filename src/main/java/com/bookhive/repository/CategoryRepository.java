
package com.bookhive.repository;

import com.bookhive.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<Category, Long>,
                JpaSpecificationExecutor<Category> {

    // Check duplicate category name
    boolean existsByName(String name);

    // Check duplicate slug
    boolean existsBySlug(String slug);

    // Find category by name
    Optional<Category> findByName(String name);

    // Find category by slug
    Optional<Category> findBySlug(String slug);

    // Search categories
    List<Category> findByNameContainingIgnoreCase(String name);

    // Find categories by target audience
    List<Category> findByTargetAudienceIgnoreCase(
            String targetAudience
    );

    // Get active categories
    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    // Count active categories
    long countByActiveTrue();
}
