package com.bookhive.repository;

import com.bookhive.entity.BookCopy;
import com.bookhive.enums.CopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    long countByBatchBookIdAndStatus(Long bookId, CopyStatus status);
    long countByBatchBookId(Long bookId);
    long countByStatus(CopyStatus status);
    long countByBatchBookActiveTrueAndStatus(CopyStatus status);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"batch", "batch.book"})
    List<BookCopy> findByBatchBookIdOrderByIdAsc(Long bookId);
    List<BookCopy> findByBatchIdOrderByIdAsc(Long batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.batch.book.id = :bookId and c.status = :status order by c.id")
    List<BookCopy> lockAvailable(@Param("bookId") Long bookId,
            @Param("status") CopyStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c where c.id = :id")
    java.util.Optional<BookCopy> lockById(@Param("id") Long id);
}
