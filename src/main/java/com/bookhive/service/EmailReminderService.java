
package com.bookhive.service;

import com.bookhive.entity.Loan;
import com.bookhive.enums.LoanStatus;
import com.bookhive.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.mail.enabled",
    havingValue = "true"
)
public class EmailReminderService {

    private final LoanRepository loans;
    private final JavaMailSender mailSender;

    @Scheduled(
        cron = "0 0 8 * * *",
        zone = "Asia/Ho_Chi_Minh"
    )
    public void sendReminders() {
        LocalDate today = LocalDate.now();

        List<Loan> active = loans.findAll()
            .stream()
            .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
            .toList();

        for (Loan loan : active) {
            boolean nearDue = !loan.getDueDate().isBefore(today)
                && !loan.getDueDate().isAfter(
                    today.plusDays(2)
                );

            boolean overdue = loan.getDueDate().isBefore(today);

            if (!nearDue && !overdue) {
                continue;
            }

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(loan.getUser().getEmail());
            mail.setSubject("BookHive - Loan Reminder");
            mail.setText(
                "Your book is due on " + loan.getDueDate()
                + ". Please return it on time."
            );

            mailSender.send(mail);
        }
    }
}
