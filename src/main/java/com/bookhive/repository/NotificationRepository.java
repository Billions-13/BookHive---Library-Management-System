
package com.bookhive.repository;

import com.bookhive.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // Get notifications of a user
    List<Notification> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    // Get notifications with pagination
    Page<Notification> findByUserId(
            Long userId,
            Pageable pageable
    );

    // Get unread notifications
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(
            Long userId
    );

    // Count unread notifications
    long countByUserIdAndReadFalse(Long userId);

    // Get notifications by type
    List<Notification> findByUserIdAndType(
            Long userId,
            String type
    );

    // Find notifications created before a date
    List<Notification> findByCreatedAtBefore(
            LocalDateTime date
    );
}
