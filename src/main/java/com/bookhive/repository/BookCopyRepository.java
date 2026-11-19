
package com.bookhive.repository;

import com.bookhive.entity.BookCopy;
import com.bookhive.enums.CopyStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface BookCopyRepository
        extends JpaRepository<BookCopy, Long> {

    long countByBookIdAndStatus(
        Long bookId, CopyStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<BookCopy> findByBookIdAndStatusOrderByIdAsc(
        Long bookId, CopyStatus status, Pageable pageable
    );
}
