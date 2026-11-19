
package com.bookhive.entity;

import com.bookhive.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "loans")
@Getter @Setter @NoArgsConstructor
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @Column(name = "borrowed_at")
    private LocalDateTime borrowedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @ManyToOne
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @ManyToOne
    @JoinColumn(name = "returned_by")
    private User returnedBy;
}
