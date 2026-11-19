
package com.bookhive.entity;

import com.bookhive.enums.CopyStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_copies")
@Getter @Setter @NoArgsConstructor
public class BookCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(name = "batch_id")
    private BookBatch batch;

    @Column(name = "copy_code", unique = true, nullable = false)
    private String copyCode;

    @Enumerated(EnumType.STRING)
    private CopyStatus status;

    @Column(name = "acquired_at")
    private LocalDate acquiredAt;

    @Column(name = "condition_note")
    private String conditionNote;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Version
    private Long version;
}
