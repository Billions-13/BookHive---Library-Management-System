package com.bookhive.entity;

import com.bookhive.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter @Setter @NoArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(name = "hold_until")
    private LocalDateTime holdUntil;
}