
package com.bookhive.repository;

import com.bookhive.entity.BookBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookBatchRepository
        extends JpaRepository<BookBatch, Long> {

    // Check duplicate batch code
    boolean existsByBatchCode(String batchCode);

    // Find batch by code
    Optional<BookBatch> findByBatchCode(String batchCode);

    // Get all batches of a book
    List<BookBatch> findByBookIdOrderByReceivedAtDesc(
            Long bookId
    );

    // Get batches created by a librarian
    List<BookBatch> findByCreatedByIdOrderByReceivedAtDesc(
            Long userId
    );

    // Find batches by supplier
    List<BookBatch> findBySupplierContainingIgnoreCase(
            String supplier
    );

    // Find batches received between two dates
    List<BookBatch> findByReceivedAtBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Count batches of a book
    long countByBookId(Long bookId);
}
