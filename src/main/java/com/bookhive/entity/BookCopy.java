package com.bookhive.entity;

import com.bookhive.enums.CopyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_copies")
@Getter @Setter @NoArgsConstructor
public class BookCopy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 3NF: book_id is available through batch.book; do NOT add book_id to book_copies.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private BookBatch batch;

    @Column(name = "copy_code", nullable = false, unique = true, length = 40)
    private String copyCode;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Column(name = "acquired_at")
    private LocalDate acquiredAt;

    @Column(name = "condition_note", length = 500)
    private String conditionNote;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Version
    private Long version;
}
