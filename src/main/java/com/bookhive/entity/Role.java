package com.bookhive.entity;

import com.bookhive.enums.RoleName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true)
    private RoleName name;

    @Column(name = "loan_limit", nullable = false)
    private int loanLimit;

    @Column(name = "loan_days", nullable = false)
    private int loanDays;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
