
package com.bookhive.repository;

import com.bookhive.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    // Get all actions performed by a user
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(
            Long actorId
    );

    // Find logs by action
    List<AuditLog> findByActionOrderByCreatedAtDesc(
            String action
    );

    // Find logs of a specific entity
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            Long entityId
    );

    // Find logs in a date range
    List<AuditLog> findByCreatedAtBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Get logs with pagination
    Page<AuditLog> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    // Count actions by type
    long countByAction(String action);
}
