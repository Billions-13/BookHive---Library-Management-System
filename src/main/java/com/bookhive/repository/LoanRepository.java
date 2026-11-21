package com.bookhive.repository;

import com.bookhive.entity.Loan;
import com.bookhive.enums.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    long countByUserIdAndStatus(Long userId, LoanStatus status);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"copy", "copy.batch", "copy.batch.book", "user"})
    List<Loan> findByUserIdOrderByBorrowedAtDesc(Long userId);
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"copy", "copy.batch", "copy.batch.book", "user"})
    List<Loan> findAll();
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);
    long countByStatusAndDueDateBefore(LoanStatus status, LocalDate date);
    boolean existsByCopyIdAndStatus(Long copyId, LoanStatus status);
    boolean existsByCopyBatchBookIdAndStatus(Long bookId, LoanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> lockById(@Param("id") Long id);
}
