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
    private final ReservationService queue;
    private final AuditService audit;

    @Transactional
    public Long borrow(Long bookId, Long readerId, User issuedBy) {
        Book book = books.lockById(bookId).orElseThrow(() -> new NotFoundException("Không tìm thấy đầu sách"));
        if (!book.isActive()) throw new IllegalArgumentException("Đầu sách đã bị ẩn");
        User reader = users.findById(readerId).orElseThrow(() -> new NotFoundException("Không tìm thấy bạn đọc"));
        if (!reader.isEnabled()) throw new IllegalArgumentException("Tài khoản bạn đọc đang bị khoá");
        if (reader.getRole().getName() != RoleName.STUDENT &&
            reader.getRole().getName() != RoleName.LECTURER)
            throw new IllegalArgumentException("Chỉ sinh viên và giảng viên được mượn sách");
        if (loans.countByUserIdAndStatus(readerId, LoanStatus.ACTIVE) >= reader.getRole().getLoanLimit())
            throw new IllegalArgumentException("Bạn đọc đã mượn đủ số sách tối đa theo vai trò");
        BookCopy copy;
        var ready = queue.readyFor(bookId, readerId);
        if (ready.isPresent()) {
            Reservation r = ready.get();
            copy = copies.lockById(r.getCopy().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cuốn sách"));
            if (copy.getStatus() != CopyStatus.RESERVED) throw new IllegalArgumentException("Cuốn sách giữ cho bạn đọc không còn khả dụng");
            r.setStatus(ReservationStatus.FULFILLED);
            r.setHoldUntil(null);
        } else {
            if (queue.hasWaitingQueue(bookId))
                throw new IllegalArgumentException("Sách đang có người đặt trước; cần phục vụ hàng đợi trước");
            copy = copies.lockAvailable(bookId, CopyStatus.AVAILABLE, PageRequest.of(0, 1))
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Hết sách – hiện không có sách để mượn"));
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
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu mượn"));
        books.lockById(firstRead.getCopy().getBatch().getBook().getId());
        Loan loan = loans.lockById(loanId).orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu mượn"));
        if (loan.getStatus() != LoanStatus.ACTIVE) throw new IllegalArgumentException("Phiếu mượn này đã được trả");
        BookCopy copy = copies.lockById(loan.getCopy().getId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy cuốn sách"));
        loan.setReturnDate(LocalDate.now());
        loan.setReturnedBy(returnedBy);
        loan.setStatus(LoanStatus.RETURNED);
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now()));
        if (daysLate > 0) {
            BigDecimal amount = DAILY_FINE_VND.multiply(BigDecimal.valueOf(daysLate));
            Fine fine = fines.findByLoanId(loanId).orElseGet(Fine::new);
            fine.setLoan(loan);
            if (fine.getId() != null && fine.getStatus() != FineStatus.UNPAID)
                throw new IllegalArgumentException("Khoản phạt của phiếu này đã được xử lý; cần thủ thư kiểm tra lại");
            fine.setAmount(amount);
            fine.setStatus(FineStatus.UNPAID);
            fine.setNotes("Trả trễ " + daysLate + " ngày × " + DAILY_FINE_VND + " ₫/ngày");
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
