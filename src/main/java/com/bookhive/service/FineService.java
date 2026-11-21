package com.bookhive.service;

import com.bookhive.entity.Fine;
import com.bookhive.enums.FineStatus;
import com.bookhive.exception.NotFoundException;
import com.bookhive.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FineService {
    private final FineRepository fines;
    private final CurrentUserService current;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<Fine> forUser(Long userId) {
        return fines.findByLoanUserIdOrderByCreatedAtDesc(userId);
    }
    @Transactional(readOnly = true)
    public List<Fine> all() { return fines.findAll(); }
    @Transactional
    public void updateStatus(Long fineId, FineStatus status) {
        if (status != FineStatus.PAID && status != FineStatus.WAIVED)
            throw new IllegalArgumentException("Only PAID or WAIVED permitted");
        Fine fine = fines.findById(fineId).orElseThrow(() -> new NotFoundException("Fine not found"));
        if (fine.getStatus() != FineStatus.UNPAID) throw new IllegalArgumentException("Fine already resolved");
        fine.setStatus(status);
        if (status == FineStatus.PAID) fine.setPaidAt(LocalDateTime.now());
        audit.log(current.requireUser(), "FINE_RESOLVED", "Fine", fineId, status.name());
    }
}
