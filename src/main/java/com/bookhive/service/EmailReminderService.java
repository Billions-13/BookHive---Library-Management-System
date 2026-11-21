package com.bookhive.service;

import com.bookhive.entity.*;
import com.bookhive.enums.LoanStatus;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailReminderService {
    private final LoanRepository loans;
    private final NotificationRepository notifications;
    private final JavaMailSender sender;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        for (Loan loan : loans.findAll()) {
            if (loan.getStatus() != LoanStatus.ACTIVE || loan.getDueDate() == null) continue;
            if (loan.getDueDate().isAfter(today.plusDays(2))) continue;
            User reader = loan.getUser();
            // Development seed uses invalid example.test mailboxes; never send to them.
            if (!reader.isEnabled() || reader.getEmail().endsWith(".example.test")) continue;
            String message = "Loan #" + loan.getId() + ": due date " + loan.getDueDate()
                + (loan.getDueDate().isBefore(today) ? " (overdue)" : " (due soon)");
            String type = "DUE_REMINDER";
            if (notifications.existsByUserIdAndTypeAndMessage(reader.getId(), type, message)) continue;
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(reader.getEmail());
            mail.setSubject("BookHive loan due date");
            mail.setText(message);
            sender.send(mail);
            Notification n = new Notification();
            n.setUser(reader); n.setTitle("Loan due reminder");
            n.setMessage(message); n.setType(type);
            n.setSentAt(java.time.LocalDateTime.now());
            notifications.save(n);
        }
    }
}
