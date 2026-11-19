package com.bookhive.repository;

import com.bookhive.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
    List<Notification> findByUserIdAndType(Long userId, String type);
    boolean existsByUserIdAndTypeAndMessage(Long userId, String type, String message);
    List<Notification> findByCreatedAtBefore(LocalDateTime date);
}
