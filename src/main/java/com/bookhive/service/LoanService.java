package com.bookhive.service;

import com.bookhive.entity.*;
import com.bookhive.enums.*;
import com.bookhive.exception.NotFoundException;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LoanService {
    public static final BigDecimal DAILY_FINE_VND = BigDecimal.valueOf(2000);
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final UserRepository users;
    private final FineRepository fines;
    private final ReservationRepository reservations;
    private final ReservationService queue;
    private final AuditService audit;

    @Transactional
    public Long borrow(Long bookId, Long readerId, User issuedBy) {
        Book book = books.lockById(bookId).orElseThrow(() -> new NotFoundException("Book not found"));
        if (!book.isActive()) throw new IllegalArgumentException("Book is archived");
        User reader = users.findById(readerId).orElseThrow(() -> new NotFoundException("Reader not found"));
        if (!reader.isEnabled()) throw new IllegalArgumentException("Reader is disabled");
        if (reader.getRole().getName() != RoleName.STUDENT &&
            reader.getRole().getName() != RoleName.LECTURER)
            throw new IllegalArgumentException("Only students/lecturers can borrow");
        if (loans.countByUserIdAndStatus(readerId, LoanStatus.ACTIVE) >= reader.getRole().getLoanLimit())
            throw new IllegalArgumentException("Loan limit reached");
        BookCopy copy;
        var ready = queue.readyFor(bookId, readerId);
        if (ready.isPresent()) {
            Reservation r = ready.get();
            copy = copies.lockById(r.getCopy().getId())
                .orElseThrow(() -> new NotFoundException("Copy not found"));
            if (copy.getStatus() != CopyStatus.RESERVED) throw new IllegalArgumentException("Hold is unavailable");
            r.setStatus(ReservationStatus.FULFILLED);
            r.setHoldUntil(null);
        } else {
            if (queue.hasWaitingQueue(bookId))
                throw new IllegalArgumentException("Existing reservation queue must be served first");
            copy = copies.lockAvailable(bookId, CopyStatus.AVAILABLE, PageRequest.of(0, 1))
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("No copies available"));
        }
        copy.setStatus(CopyStatus.BORROWED);
        Loan loan = new Loan();
        loan.setUser(reader); loan.setCopy(copy);
        loan.setBorrowedAt(LocalDateTime.now());
        loan.setDueDate(LocalDate.now().plusDays(reader.getRole().getLoanDays()));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setIssuedBy(issuedBy);
        loans.saveAndFlush(loan); // DB unique generated column blocks double active checkout.
        audit.log(issuedBy, "LOAN_ISSUED", "Loan", loan.getId(), copy.getCopyCode());
        return loan.getId();
    }

    @Transactional
    public void returnCopy(Long loanId, User returnedBy) {
        Loan firstRead = loans.findById(loanId)
            .orElseThrow(() -> new NotFoundException("Loan not found"));
        books.lockById(firstRead.getCopy().getBatch().getBook().getId());
        Loan loan = loans.lockById(loanId).orElseThrow(() -> new NotFoundException("Loan not found"));
        if (loan.getStatus() != LoanStatus.ACTIVE) throw new IllegalArgumentException("Loan already returned");
        BookCopy copy = copies.lockById(loan.getCopy().getId())
            .orElseThrow(() -> new NotFoundException("Copy not found"));
        loan.setReturnDate(LocalDate.now());
        loan.setReturnedBy(returnedBy);
        loan.setStatus(LoanStatus.RETURNED);
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now()));
        if (daysLate > 0) {
            BigDecimal amount = DAILY_FINE_VND.multiply(BigDecimal.valueOf(daysLate));
            Fine fine = fines.findByLoanId(loanId).orElseGet(Fine::new);
            fine.setLoan(loan);
            if (fine.getId() != null && fine.getStatus() != FineStatus.UNPAID)
                throw new IllegalArgumentException("Loan fine already settled; staff review required");
            fine.setAmount(amount);
            fine.setStatus(FineStatus.UNPAID);
            fine.setNotes(daysLate + " day(s) late at " + DAILY_FINE_VND + " VND/day");
            fines.save(fine);
        }
        queue.offerToNext(copy);
        audit.log(returnedBy, "LOAN_RETURNED", "Loan", loanId, copy.getCopyCode());
    }

    @Transactional(readOnly = true)
    public List<Loan> forUser(Long userId) {
        return loans.findByUserIdOrderByBorrowedAtDesc(userId);
    }
    @Transactional(readOnly = true)
    public List<Loan> all() {
        return loans.findAll();
    }
}
