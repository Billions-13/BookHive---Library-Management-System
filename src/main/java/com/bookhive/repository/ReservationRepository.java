package com.bookhive.repository;

import com.bookhive.entity.Reservation;
import com.bookhive.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAscIdAsc(Long bookId, ReservationStatus status);
    Optional<Reservation> findFirstByBookIdAndStatusInOrderByReservedAtAscIdAsc(Long bookId, Collection<ReservationStatus> statuses);
    boolean existsByBookIdAndUserIdAndStatusIn(Long bookId, Long userId, Collection<ReservationStatus> statuses);
    boolean existsByBookIdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);
    List<Reservation> findByUserIdOrderByReservedAtDesc(Long userId);
    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.holdUntil < :time")
    List<Reservation> findByStatusAndHoldUntilBefore(ReservationStatus status, LocalDateTime time);
}
