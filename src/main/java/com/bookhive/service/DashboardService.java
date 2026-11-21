
package com.bookhive.service;

import com.bookhive.dto.DashboardCharts;
import com.bookhive.dto.DashboardStats;
import com.bookhive.enums.*;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    /** How many months the "loans per month" chart covers, current month included. */
    static final int MONTHS = 6;
    static final int TOP_BOOKS = 5;

    private final BookRepository books;
    private final BookCopyRepository copies;
    private final LoanRepository loans;

    public DashboardStats getStats() {
        return new DashboardStats(
            books.countByActiveTrue(),
            copies.countByBatchBookActiveTrueAndStatus(CopyStatus.AVAILABLE),
            copies.countByBatchBookActiveTrueAndStatus(CopyStatus.BORROWED),
            loans.countByStatusAndDueDateBefore(
                LoanStatus.ACTIVE, LocalDate.now()
            )
        );
    }

    @Transactional(readOnly = true)
    public DashboardCharts getCharts() {
        List<DashboardCharts.StatusCount> byStatus = Arrays.stream(CopyStatus.values())
            .map(s -> new DashboardCharts.StatusCount(s.name(),
                copies.countByBatchBookActiveTrueAndStatus(s)))
            .toList();

        List<DashboardCharts.MonthCount> perMonth = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            perMonth.add(new DashboardCharts.MonthCount(month.toString(),
                loans.countByBorrowedAtGreaterThanEqualAndBorrowedAtLessThan(
                    month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay())));
        }

        List<DashboardCharts.BookCount> top = loans.mostBorrowedBooks(PageRequest.of(0, TOP_BOOKS)).stream()
            .map(row -> new DashboardCharts.BookCount((Long) row[0], (String) row[1], ((Number) row[2]).longValue()))
            .toList();

        return new DashboardCharts(byStatus, perMonth, top);
    }
}
