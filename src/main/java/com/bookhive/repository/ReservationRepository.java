package com.bookhive.repository;

import com.bookhive.entity.Reservation;
import com.bookhive.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    Optional<Reservation>
    findFirstByBookIdAndStatusOrderByReservedAtAscIdAsc(
        Long bookId, ReservationStatus status
    );

    boolean existsByBookIdAndUserIdAndStatusIn(
        Long bookId,
        Long userId,
        Collection<ReservationStatus> statuses
    );

    List<Reservation>
    findByStatusAndHoldUntilBefore(
        ReservationStatus status,
        java.time.LocalDateTime time
    );
}