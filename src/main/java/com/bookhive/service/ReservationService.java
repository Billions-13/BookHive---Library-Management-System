package com.bookhive.service;

import com.bookhive.entity.*;
import com.bookhive.enums.*;
import com.bookhive.exception.NotFoundException;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservations;
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final AuditService audit;

    @Transactional
    public Long reserve(Long bookId, User reader) {
        if (!reader.isEnabled() ||
            (reader.getRole().getName() != RoleName.STUDENT && reader.getRole().getName() != RoleName.LECTURER))
            throw new IllegalArgumentException("Chỉ sinh viên và giảng viên đang hoạt động được đặt trước");
        Book book = books.lockById(bookId).orElseThrow(() -> new NotFoundException("Không tìm thấy đầu sách"));
        if (!book.isActive()) throw new IllegalArgumentException("Đầu sách đã bị ẩn");
        if (copies.countByBatchBookIdAndStatus(bookId, CopyStatus.AVAILABLE) > 0)
            throw new IllegalArgumentException("Sách vẫn còn trong kho; hãy mượn trực tiếp thay vì đặt trước");
        if (reservations.existsByBookIdAndUserIdAndStatusIn(bookId, reader.getId(),
                List.of(ReservationStatus.REQUESTED, ReservationStatus.WAITING, ReservationStatus.READY)))
            throw new IllegalArgumentException("Bạn đã đặt trước đầu sách này rồi");
        Reservation r = new Reservation();
        r.setBook(book); r.setUser(reader); r.setReservedAt(LocalDateTime.now());
        r.setStatus(ReservationStatus.WAITING);
        reservations.saveAndFlush(r);
        audit.log(reader, "RESERVATION_CREATED", "Reservation", r.getId(), book.getIsbn());
        return r.getId();
    }

    @Transactional(readOnly = true)
    public List<Reservation> forUser(Long userId) {
        return reservations.findByUserIdOrderByReservedAtDesc(userId);
    }

    @Transactional
    public void cancel(Long reservationId, User reader) {
        Reservation r = reservations.findById(reservationId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt đặt trước"));
        books.lockById(r.getBook().getId());
        if (!r.getUser().getId().equals(reader.getId()))
            throw new IllegalArgumentException("Bạn không thể huỷ lượt đặt trước của người khác");
        if (r.getStatus() != ReservationStatus.WAITING && r.getStatus() != ReservationStatus.READY &&
            r.getStatus() != ReservationStatus.REQUESTED)
            throw new IllegalArgumentException("Lượt đặt trước này không thể huỷ");
        BookCopy held = r.getCopy();
        r.setCopy(null); r.setHoldUntil(null); r.setStatus(ReservationStatus.CANCELLED);
        if (held != null) {
            held.setStatus(CopyStatus.AVAILABLE);
            offerToNext(held);
        }
        audit.log(reader, "RESERVATION_CANCELLED", "Reservation", r.getId(), "Cancelled by reader");
    }

    // Called from the same transaction as return: queue changes and copy status commit together.
    public void offerToNext(BookCopy returned) {
        Long bookId = returned.getBatch().getBook().getId();
        var next = reservations.findFirstByBookIdAndStatusInOrderByReservedAtAscIdAsc(bookId,
            List.of(ReservationStatus.WAITING, ReservationStatus.REQUESTED));
        if (next.isPresent()) {
            Reservation r = next.get();
            returned.setStatus(CopyStatus.RESERVED);
            r.setCopy(returned);
            r.setStatus(ReservationStatus.READY);
            r.setHoldUntil(LocalDateTime.now().plusDays(2));
            audit.log(r.getUser(), "RESERVATION_READY", "Reservation", r.getId(), "Ready for pickup");
        } else {
            returned.setStatus(CopyStatus.AVAILABLE);
        }
    }

    @Scheduled(cron = "0 15 1 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void expireReadyReservations() {
        for (Reservation r : reservations.findByStatusAndHoldUntilBefore(
                ReservationStatus.READY, LocalDateTime.now())) {
            books.lockById(r.getBook().getId());
            BookCopy held = r.getCopy();
            r.setCopy(null); r.setHoldUntil(null); r.setStatus(ReservationStatus.EXPIRED);
            if (held != null) offerToNext(held);
        }
    }

    public Optional<Reservation> readyFor(Long bookId, Long userId) {
        return reservations.findByUserIdOrderByReservedAtDesc(userId).stream()
            .filter(r -> r.getBook().getId().equals(bookId) &&
                r.getStatus() == ReservationStatus.READY &&
                r.getHoldUntil() != null && r.getHoldUntil().isAfter(LocalDateTime.now()))
            .findFirst();
    }
    public boolean hasWaitingQueue(Long bookId) {
        return reservations.existsByBookIdAndStatusIn(bookId,
            List.of(ReservationStatus.WAITING, ReservationStatus.REQUESTED));
    }
}
