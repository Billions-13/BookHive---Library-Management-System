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
            boolean overdue = loan.getDueDate().isBefore(today);
            String title = loan.getCopy().getBatch().getBook().getTitle();
            // Also the de-duplication key: one e-mail for "due soon" and one more once it becomes overdue.
            String message = "Phiếu mượn #" + loan.getId() + " – \"" + title + "\": hạn trả "
                + loan.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + (overdue ? " (đã quá hạn)" : " (sắp đến hạn)");
            String type = "DUE_REMINDER";
            if (notifications.existsByUserIdAndTypeAndMessage(reader.getId(), type, message)) continue;
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(reader.getEmail());
            mail.setSubject(overdue ? "[BookHive] Sách mượn đã quá hạn trả" : "[BookHive] Nhắc hạn trả sách");
            mail.setText("Chào " + reader.getFullName() + ",\n\n"
                + message + ".\n\n"
                + (overdue
                    ? "Vui lòng trả sách sớm tại quầy thư viện. Phí phạt trả trễ là "
                        + LoanService.DAILY_FINE_VND + " ₫ cho mỗi ngày quá hạn.\n\n"
                    : "Vui lòng trả sách hoặc liên hệ thủ thư trước hạn để tránh bị phạt.\n\n")
                + "Thư viện BookHive");
            sender.send(mail);
            Notification n = new Notification();
            n.setUser(reader); n.setTitle(overdue ? "Sách quá hạn trả" : "Nhắc hạn trả sách");
            n.setMessage(message); n.setType(type);
            n.setSentAt(java.time.LocalDateTime.now());
            notifications.save(n);
        }
    }
}
