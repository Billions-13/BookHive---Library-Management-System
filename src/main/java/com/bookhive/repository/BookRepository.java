
package com.bookhive.repository;

import com.bookhive.entity.Book;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface BookRepository
        extends JpaRepository<Book, Long>,
                JpaSpecificationExecutor<Book> {

    boolean existsByIsbn(String isbn);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> lockById(@Param("id") Long id);

    long countByActiveTrue();
}
