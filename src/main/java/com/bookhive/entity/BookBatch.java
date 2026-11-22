
package com.bookhive.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_batches")
@Getter @Setter @NoArgsConstructor
public class BookBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "batch_code", unique = true, nullable = false)
    private String batchCode;

    private int quantity;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    private String supplier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
