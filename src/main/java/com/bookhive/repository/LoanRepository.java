package com.bookhive.repository;

import com.bookhive.entity.Loan;
import com.bookhive.enums.LoanStatus;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.*;

public interface LoanRepository
        extends JpaRepository<Loan, Long> {

    long countByUserIdAndStatus(
        Long userId, LoanStatus status
    );

    List<Loan> findByUserIdOrderByBorrowedAtDesc(Long userId);

    List<Loan> findByStatusAndDueDateBefore(
        LoanStatus status, java.time.LocalDate date
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.id = :id")
    Optional<Loan> lockById(Long id);


    long countByStatusAndDueDateBefore(
    LoanStatus status,
    LocalDate date
);
}
