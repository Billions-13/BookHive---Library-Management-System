package com.bookhive.repository;

import com.bookhive.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuthorRepository
        extends JpaRepository<Author, Long>,
                JpaSpecificationExecutor<Author> {

    // Check duplicate author name
    boolean existsByName(String name);

    // Find authors by name
    List<Author> findByNameContainingIgnoreCase(String name);

    // Find authors by country
    List<Author> findByCountryIgnoreCase(String country);

    // Get all active authors
    List<Author> findByActiveTrueOrderByNameAsc();

    // Count active authors
    long countByActiveTrue();
}
