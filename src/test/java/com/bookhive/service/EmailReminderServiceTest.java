package com.bookhive.service;

import com.bookhive.entity.*;
import com.bookhive.enums.LoanStatus;
import com.bookhive.repository.LoanRepository;
import com.bookhive.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Automated e-mail reminders (advanced feature 3) without a real SMTP server. */
class EmailReminderServiceTest {

    private final LoanRepository loans = mock(LoanRepository.class);
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final EmailReminderService service = new EmailReminderService(loans, notifications, sender);

    private static Loan loan(long id, String email, LocalDate due, LoanStatus status) {
        User reader = new User();
        reader.setId(id);
        reader.setFullName("Nguyễn Văn A");
        reader.setEmail(email);
        reader.setEnabled(true);
        Book book = new Book();
        book.setTitle("Lập trình Java căn bản");
        BookBatch batch = new BookBatch();
        batch.setBook(book);
        BookCopy copy = new BookCopy();
        copy.setBatch(batch);
        Loan loan = new Loan();
        loan.setId(id);
        loan.setUser(reader);
        loan.setCopy(copy);
        loan.setDueDate(due);
        loan.setStatus(status);
        return loan;
    }

    @Test
    void sendsVietnameseOverdueReminderAndRecordsNotification() {
        when(loans.findAll()).thenReturn(List.of(loan(1, "reader@hcmiu.edu.vn", LocalDate.now().minusDays(3), LoanStatus.ACTIVE)));
        when(notifications.existsByUserIdAndTypeAndMessage(anyLong(), anyString(), anyString())).thenReturn(false);

        service.sendReminders();

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(mail.capture());
        assertArrayEquals(new String[]{"reader@hcmiu.edu.vn"}, mail.getValue().getTo());
        assertEquals("[BookHive] Sách mượn đã quá hạn trả", mail.getValue().getSubject());
        assertTrue(mail.getValue().getText().contains("Lập trình Java căn bản"));
        assertTrue(mail.getValue().getText().contains("đã quá hạn"));
        verify(notifications).save(any(Notification.class));
    }

    @Test
    void remindsTwoDaysBeforeDueDate() {
        when(loans.findAll()).thenReturn(List.of(loan(2, "reader@hcmiu.edu.vn", LocalDate.now().plusDays(2), LoanStatus.ACTIVE)));

        service.sendReminders();

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(mail.capture());
        assertEquals("[BookHive] Nhắc hạn trả sách", mail.getValue().getSubject());
        assertTrue(mail.getValue().getText().contains("sắp đến hạn"));
    }

    @Test
    void skipsReturnedFarAwayDemoAndAlreadyNotifiedLoans() {
        when(loans.findAll()).thenReturn(List.of(
            loan(3, "reader@hcmiu.edu.vn", LocalDate.now().minusDays(1), LoanStatus.RETURNED),
            loan(4, "reader@hcmiu.edu.vn", LocalDate.now().plusDays(10), LoanStatus.ACTIVE),
            loan(5, "student@bookhive.example.test", LocalDate.now().minusDays(1), LoanStatus.ACTIVE),
            loan(6, "notified@hcmiu.edu.vn", LocalDate.now().minusDays(1), LoanStatus.ACTIVE)));
        when(notifications.existsByUserIdAndTypeAndMessage(eq(6L), anyString(), anyString())).thenReturn(true);

        service.sendReminders();

        verify(sender, never()).send(any(SimpleMailMessage.class));
        verify(notifications, never()).save(any());
    }
}
