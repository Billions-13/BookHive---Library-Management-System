
package com.bookhive.repository;

import com.bookhive.entity.Fine;
import com.bookhive.enums.FineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FineRepository
        extends JpaRepository<Fine, Long> {

    // Find fine by loan
    Optional<Fine> findByLoanId(Long loanId);

    // Check if a loan already has a fine
    boolean existsByLoanId(Long loanId);

    // Get all fines by status
    List<Fine> findByStatus(FineStatus status);

    // Get fines of a user (book title is shown, so load loan -> copy -> batch -> book;
    // production runs with open-in-view=false)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
        "loan", "loan.user", "loan.copy", "loan.copy.batch", "loan.copy.batch.book"})
    List<Fine> findByLoanUserIdOrderByCreatedAtDesc(
            Long userId
    );

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
        "loan", "loan.user", "loan.copy", "loan.copy.batch", "loan.copy.batch.book"})
    List<Fine> findAll();

    // Get unpaid fines of a user
    List<Fine> findByLoanUserIdAndStatus(
            Long userId,
            FineStatus status
    );

    // Get paginated fines
    Page<Fine> findByStatus(
            FineStatus status,
            Pageable pageable
    );

    // Count fines by status
    long countByStatus(FineStatus status);
}
