package com.bookhive.service;

import com.bookhive.dto.BatchForm;
import com.bookhive.entity.*;
import com.bookhive.enums.CopyStatus;
import com.bookhive.enums.LoanStatus;
import com.bookhive.exception.NotFoundException;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final BookRepository books;
    private final BookBatchRepository batches;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final AuditService audit;
    private final ReservationService queue;
    private final CurrentUserService current;

    @Transactional
    public Long receive(Long bookId, BatchForm form) {
        if (form.getQuantity() == null || form.getQuantity() < 1 || form.getQuantity() > 1000)
            throw new IllegalArgumentException("Quantity must be 1–1000");
        Book book = books.lockById(bookId).orElseThrow(() -> new NotFoundException("Book not found"));
        if (!book.isActive()) throw new IllegalArgumentException("Book is archived");
        User actor = current.requireUser();
        BookBatch batch = new BookBatch();
        batch.setBook(book);
        batch.setBatchCode("BATCH-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
        batch.setQuantity(form.getQuantity());
        batch.setReceivedAt(LocalDateTime.now());
        batch.setSupplier(form.getSupplier());
        batch.setNotes(form.getNotes());
        batch.setCreatedBy(actor);
        batches.save(batch);
        for (int i = 0; i < form.getQuantity(); i++) {
            BookCopy copy = new BookCopy();
            copy.setBatch(batch);
            // Two steps because copy_code is NOT NULL but id is assigned by MySQL on insert.
            // The final code is deterministic and unique: COPY010191 for physical copy ID 10191.
            copy.setCopyCode("TEMP-" + UUID.randomUUID().toString().substring(0, 24).toUpperCase(Locale.ROOT));
            copy.setStatus(CopyStatus.AVAILABLE);
            copy.setAcquiredAt(LocalDate.now());
            copies.saveAndFlush(copy);
            copy.setCopyCode("COPY" + String.format(Locale.ROOT, "%06d", copy.getId()));
            // Offer newly arrived copies to the oldest waiting reservations first.
            queue.offerToNext(copy);
        }
        audit.log(actor, "BATCH_RECEIVED", "BookBatch", batch.getId(),
            "ISBN=" + book.getIsbn() + "; quantity=" + form.getQuantity());
        return batch.getId();
    }

    @Transactional(readOnly = true)
    public List<BookBatch> batches(Long bookId) {
        return batches.findByBookIdOrderByReceivedAtDesc(bookId);
    }

    @Transactional(readOnly = true)
    public List<BookCopy> copies(Long bookId) {
        return copies.findByBatchBookIdOrderByIdAsc(bookId);
    }

    @Transactional
    public void changeStatus(Long bookId, Long copyId, CopyStatus status, String note) {
        if (status != CopyStatus.AVAILABLE && status != CopyStatus.DAMAGED &&
            status != CopyStatus.LOST && status != CopyStatus.DISCARDED)
            throw new IllegalArgumentException("Use borrowing/reservation flows for status changes");
        BookCopy copy = copies.lockById(copyId)
            .orElseThrow(() -> new NotFoundException("Copy not found"));
        if (!copy.getBatch().getBook().getId().equals(bookId))
            throw new IllegalArgumentException("Copy does not belong to this book");
        if (loans.existsByCopyIdAndStatus(copyId, LoanStatus.ACTIVE) ||
            copy.getStatus() == CopyStatus.BORROWED || copy.getStatus() == CopyStatus.RESERVED)
            throw new IllegalArgumentException("Cannot change a borrowed or reserved copy");
        if (copy.getStatus() == CopyStatus.DISCARDED)
            throw new IllegalArgumentException("Already discarded; import a new copy instead");
        copy.setStatus(status);
        copy.setConditionNote(note);
        if (status == CopyStatus.DISCARDED) copy.setRemovedAt(LocalDateTime.now());
        audit.log(current.requireUser(), "COPY_STATUS_CHANGED", "BookCopy", copyId, status.name());
    }

    @Transactional
    public void discardBatch(Long batchId) {
        BookBatch batch = batches.findById(batchId)
            .orElseThrow(() -> new NotFoundException("Batch not found"));
        // Lock the book so issuance and batch replacement cannot race.
        books.lockById(batch.getBook().getId());
        User actor = current.requireUser();
        var all = copies.findByBatchIdOrderByIdAsc(batchId);
        for (BookCopy c : all) {
            if (c.getStatus() == CopyStatus.BORROWED || c.getStatus() == CopyStatus.RESERVED ||
                loans.existsByCopyIdAndStatus(c.getId(), LoanStatus.ACTIVE))
                throw new IllegalArgumentException("Cannot replace a batch with borrowed/reserved copies");
        }
        for (BookCopy c : all) {
            if (c.getStatus() != CopyStatus.DISCARDED) {
                c.setStatus(CopyStatus.DISCARDED);
                c.setRemovedAt(LocalDateTime.now());
            }
        }
        audit.log(actor, "BATCH_DISCARDED", "BookBatch", batchId, "Replaced/retired old batch");
    }
}
