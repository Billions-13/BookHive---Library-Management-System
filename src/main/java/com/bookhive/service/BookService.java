package com.bookhive.service;

import com.bookhive.dto.*;
import com.bookhive.entity.Book;
import com.bookhive.enums.CopyStatus;
import com.bookhive.exception.NotFoundException;
import com.bookhive.repository.*;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository books;
    private final AuthorRepository authors;
    private final ReservationRepository reservations;
    private final CategoryRepository categories;
    private final BookCopyRepository copies;
    private final AuditService audit;
    private final CurrentUserService current;

    @Transactional(readOnly = true)
    public Page<BookRow> search(String title, String isbn, String author, String category,
                 String publisher, String language, Integer year, int page, int size, String sort,
                 String direction) {
        if (!List.of(10, 20, 50, 100).contains(size)) size = 10;
        page = Math.max(0, page);
        if (!Set.of("title", "isbn", "publicationYear", "publisher", "id").contains(sort)) sort = "title";
        Sort.Order order = "desc".equalsIgnoreCase(direction)
            ? Sort.Order.desc(sort) : Sort.Order.asc(sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(order, Sort.Order.asc("id")));
        Specification<Book> spec = (root, query, builder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.isTrue(root.get("active")));
            if (has(title)) predicates.add(builder.like(builder.lower(root.get("title")), like(title)));
            if (has(isbn)) predicates.add(builder.like(builder.lower(root.get("isbn")), like(isbn)));
            if (has(publisher)) predicates.add(builder.like(builder.lower(root.get("publisher")), like(publisher)));
            if (has(language)) predicates.add(builder.like(builder.lower(root.get("language")), like(language)));
            if (year != null) predicates.add(builder.equal(root.get("publicationYear"), year));
            if (has(author) || has(category)) {
                // Subqueries avoid duplicate rows caused by JOINs into many-to-many relationships.
                if (has(author)) {
                    var sub = query.subquery(Long.class);
                    var aBook = sub.from(Book.class);
                    var a = aBook.join("authors", JoinType.INNER);
                    sub.select(aBook.get("id")).where(builder.equal(aBook.get("id"), root.get("id")),
                        builder.like(builder.lower(a.get("name")), like(author)));
                    predicates.add(builder.exists(sub));
                }
                if (has(category)) {
                    var sub = query.subquery(Long.class);
                    var cBook = sub.from(Book.class);
                    var c = cBook.join("categories", JoinType.INNER);
                    sub.select(cBook.get("id")).where(builder.equal(cBook.get("id"), root.get("id")),
                        builder.like(builder.lower(c.get("name")), like(category)));
                    predicates.add(builder.exists(sub));
                }
            }
            return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return books.findAll(spec, pageable).map(this::toRow);
    }

    private static boolean has(String s) { return s != null && !s.isBlank(); }
    private static String like(String s) {
        return "%" + s.trim().toLowerCase(Locale.ROOT)
             + "%";
    }
    private BookRow toRow(Book book) {
        return new BookRow(book.getId(), book.getIsbn(), book.getTitle(),
            String.join(", ", book.getAuthors().stream().map(a -> a.getName()).sorted().toList()),
            book.getPublisher(), book.getLanguage(),
            book.getPublicationYear() == null ? 0 : book.getPublicationYear(),
            copies.countByBatchBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE));
    }

    @Transactional(readOnly = true)
    public BookDetails details(Long id) {
        Book b = findActive(id);
        return new BookDetails(b.getId(), b.getIsbn(), b.getTitle(), b.getEdition(),
            b.getPublisher(), b.getPublicationYear(), b.getLanguage(), b.getDescription(),
            b.getCoverUrl(), b.getAuthors().stream().map(a -> a.getName()).sorted().toList(),
            b.getCategories().stream().map(c -> c.getName()).sorted().toList(),
            copies.countByBatchBookId(id), copies.countByBatchBookIdAndStatus(id, CopyStatus.AVAILABLE),
            copies.countByBatchBookIdAndStatus(id, CopyStatus.BORROWED),
            copies.countByBatchBookIdAndStatus(id, CopyStatus.DAMAGED));
    }
    @Transactional(readOnly = true)
    public List<BookRow> exportAll() {
        return books.findAllById(books.findAll().stream().filter(Book::isActive).map(Book::getId).toList())
            .stream().map(this::toRow).toList();
    }
    @Transactional(readOnly = true)
    public BookForm form(Long id) {
        Book b = findActive(id);
        BookForm f = new BookForm();
        f.setIsbn(b.getIsbn()); f.setTitle(b.getTitle()); f.setEdition(b.getEdition());
        f.setPublisher(b.getPublisher()); f.setPublicationYear(b.getPublicationYear());
        f.setLanguage(b.getLanguage()); f.setDescription(b.getDescription()); f.setCoverUrl(b.getCoverUrl());
        f.setAuthorIds(b.getAuthors().stream().map(a -> a.getId()).toList());
        f.setCategoryIds(b.getCategories().stream().map(c -> c.getId()).toList());
        return f;
    }
    @Transactional
    public Long create(BookForm f) {
        validate(f);
        String isbn = f.getIsbn().trim();
        if (books.existsByIsbn(isbn)) throw new IllegalArgumentException("ISBN already exists");
        Book b = new Book();
        apply(b, f); books.saveAndFlush(b);
        audit.log(current.requireUser(), "BOOK_CREATED", "Book", b.getId(), b.getIsbn());
        return b.getId();
    }
    @Transactional
    public void edit(Long id, BookForm f) {
        validate(f);
        Book b = findActive(id);
        if (copies.countByBatchBookId(id) > 0 &&
            (!b.getIsbn().equals(f.getIsbn().trim()) ||
             !java.util.Objects.equals(b.getEdition(), f.getEdition())))
            throw new IllegalArgumentException("Different ISBN/edition requires a new book title and a new batch");
        if (books.existsByIsbnAndIdNot(f.getIsbn().trim(), id))
            throw new IllegalArgumentException("ISBN already exists");
        apply(b, f);
        audit.log(current.requireUser(), "BOOK_UPDATED", "Book", b.getId(), b.getIsbn());
    }
    @Transactional
    public void archive(Long id) {
        Book b = findActive(id);
        if (copies.countByBatchBookIdAndStatus(id, CopyStatus.BORROWED) > 0 ||
            copies.countByBatchBookIdAndStatus(id, CopyStatus.RESERVED) > 0 ||
            reservations.existsByBookIdAndStatusIn(id, java.util.List.of(
                com.bookhive.enums.ReservationStatus.WAITING,
                com.bookhive.enums.ReservationStatus.REQUESTED,
                com.bookhive.enums.ReservationStatus.READY)))
            throw new IllegalArgumentException("Cannot archive a book with active loans/holds/reservations");
        b.setActive(false);
        audit.log(current.requireUser(), "BOOK_ARCHIVED", "Book", id, b.getIsbn());
    }
    @Transactional(readOnly = true)
    public Book findActive(Long id) {
        Book b = books.findById(id).orElseThrow(() -> new NotFoundException("Book not found"));
        if (!b.isActive()) throw new NotFoundException("Book is archived");
        return b;
    }
    private void validate(BookForm f) {
        if (f.getIsbn() == null || f.getIsbn().isBlank() || f.getIsbn().length() > 20 ||
            f.getTitle() == null || f.getTitle().isBlank() || f.getTitle().length() > 255)
            throw new IllegalArgumentException("ISBN and title required (within length limits)");
        if (f.getPublicationYear() != null &&
            (f.getPublicationYear() < 1400 || f.getPublicationYear() > 2100))
            throw new IllegalArgumentException("Invalid publication year");
        if (f.getPublisher() != null && f.getPublisher().length() > 150)
            throw new IllegalArgumentException("Publisher too long");
        if (f.getLanguage() != null && f.getLanguage().length() > 50)
            throw new IllegalArgumentException("Language too long");
        if (f.getCoverUrl() != null && !f.getCoverUrl().isBlank() &&
            !(f.getCoverUrl().startsWith("https://") || f.getCoverUrl().startsWith("http://")))
            throw new IllegalArgumentException("Invalid cover URL");
    }
    private void apply(Book b, BookForm f) {
        b.setIsbn(f.getIsbn().trim()); b.setTitle(f.getTitle().trim());
        b.setEdition(f.getEdition()); b.setPublisher(f.getPublisher());
        b.setPublicationYear(f.getPublicationYear()); b.setLanguage(f.getLanguage());
        b.setDescription(f.getDescription()); b.setCoverUrl(f.getCoverUrl());
        b.setAuthors(new java.util.HashSet<>(authors.findAllById(
            f.getAuthorIds() == null ? java.util.List.of() : f.getAuthorIds())));
        b.setCategories(new java.util.HashSet<>(categories.findAllById(
            f.getCategoryIds() == null ? java.util.List.of() : f.getCategoryIds())));
    }
}
