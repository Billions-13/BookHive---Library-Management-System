
package com.bookhive.service;

import com.bookhive.dto.DashboardStats;
import com.bookhive.enums.*;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

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
}
