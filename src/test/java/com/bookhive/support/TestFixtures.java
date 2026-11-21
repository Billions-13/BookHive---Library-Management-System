package com.bookhive.support;

import com.bookhive.entity.*;
import com.bookhive.enums.*;
import com.bookhive.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Small, deterministic data set for integration tests: one account per role,
 * a book with available copies, an out-of-stock book with a reservation queue,
 * an overdue loan and an unpaid fine. Register it with {@code @Import}.
 */
public class TestFixtures {

    public static final String PASSWORD = "BookHiveTest!2026";
    public static final String STUDENT = "student@test.local";
    public static final String LECTURER = "lecturer@test.local";
    public static final String LIBRARIAN = "librarian@test.local";
    public static final String ADMIN = "admin@test.local";

    private final RoleRepository roles;
    private final UserRepository users;
    private final AuthorRepository authors;
    private final CategoryRepository categories;
    private final BookRepository books;
    private final BookBatchRepository batches;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final FineRepository fines;
    private final ReservationRepository reservations;
    private final PasswordEncoder encoder;

    private boolean seeded;
    public Long studentId;
    public Long authorId;
    public Long categoryId;
    public Long availableBookId;
    public Long outOfStockBookId;
    public Long activeLoanId;
    public Long unpaidFineId;
    public Long reservationId;

    public TestFixtures(RoleRepository roles, UserRepository users, AuthorRepository authors,
            CategoryRepository categories, BookRepository books, BookBatchRepository batches,
            BookCopyRepository copies, LoanRepository loans, FineRepository fines,
            ReservationRepository reservations, PasswordEncoder encoder) {
        this.roles = roles;
        this.users = users;
        this.authors = authors;
        this.categories = categories;
        this.books = books;
        this.batches = batches;
        this.copies = copies;
        this.loans = loans;
        this.fines = fines;
        this.reservations = reservations;
        this.encoder = encoder;
    }

    /** Inserts the data once per Spring test context; later calls are no-ops. */
    public synchronized void seed() {
        if (seeded) return;

        Role studentRole = role(RoleName.STUDENT, 5, 14);
        Role lecturerRole = role(RoleName.LECTURER, 10, 30);
        Role librarianRole = role(RoleName.LIBRARIAN, 0, 0);
        Role adminRole = role(RoleName.ADMIN, 0, 0);

        User student = user(STUDENT, "Nguyễn Văn Sinh", studentRole, "ITITIU22001");
        user(LECTURER, "Trần Thị Giảng", lecturerRole, null);
        User librarian = user(LIBRARIAN, "Lê Thủ Thư", librarianRole, null);
        user(ADMIN, "Phạm Quản Trị", adminRole, null);
        studentId = student.getId();

        Author author = new Author();
        author.setName("Nguyễn Nhật Ánh");
        author.setCountry("Việt Nam");
        author.setBirthYear(1955);
        authors.save(author);
        authorId = author.getId();

        Category category = new Category();
        category.setName("Công nghệ thông tin");
        category.setSlug("cong-nghe-thong-tin");
        category.setTargetAudience("Sinh viên");
        categories.save(category);
        categoryId = category.getId();

        Book available = book("9780000000001", "Lập trình Java căn bản", author, category);
        BookBatch batch1 = batch(available, "BATCH-T1", 3, librarian);
        BookCopy borrowedCopy = copy(batch1, "COPYT0001", CopyStatus.BORROWED);
        copy(batch1, "COPYT0002", CopyStatus.AVAILABLE);
        copy(batch1, "COPYT0003", CopyStatus.DAMAGED);
        availableBookId = available.getId();

        Book outOfStock = book("9780000000002", "Thiết kế cơ sở dữ liệu", author, category);
        BookBatch batch2 = batch(outOfStock, "BATCH-T2", 1, librarian);
        BookCopy onlyCopy = copy(batch2, "COPYT0004", CopyStatus.BORROWED);
        outOfStockBookId = outOfStock.getId();

        // Overdue loan for the student.
        Loan overdue = loan(student, borrowedCopy, LocalDate.now().minusDays(20), LocalDate.now().minusDays(6),
            null, LoanStatus.ACTIVE, librarian);
        activeLoanId = overdue.getId();

        // The out-of-stock book is held by the student too, so another reader has to reserve it.
        loan(student, onlyCopy, LocalDate.now().minusDays(2), LocalDate.now().plusDays(12),
            null, LoanStatus.ACTIVE, librarian);

        // A returned, late loan with an unpaid fine.
        Loan returned = loan(student, borrowedCopy, LocalDate.now().minusDays(60), LocalDate.now().minusDays(46),
            LocalDate.now().minusDays(41), LoanStatus.RETURNED, librarian);
        Fine fine = new Fine();
        fine.setLoan(returned);
        fine.setAmount(new BigDecimal("10000"));
        fine.setStatus(FineStatus.UNPAID);
        fine.setNotes("5 ngày trễ hạn");
        fines.save(fine);
        unpaidFineId = fine.getId();

        User lecturer = users.findByEmail(LECTURER).orElseThrow();
        Reservation reservation = new Reservation();
        reservation.setUser(lecturer);
        reservation.setBook(outOfStock);
        reservation.setReservedAt(LocalDateTime.now().minusDays(1));
        reservation.setStatus(ReservationStatus.WAITING);
        reservations.save(reservation);
        reservationId = reservation.getId();

        seeded = true;
    }

    private Role role(RoleName name, int limit, int days) {
        Role r = new Role();
        r.setName(name);
        r.setLoanLimit(limit);
        r.setLoanDays(days);
        return roles.save(r);
    }

    private User user(String email, String name, Role role, String studentCode) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setStudentCode(studentCode);
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setEnabled(true);
        return users.save(u);
    }

    private Book book(String isbn, String title, Author author, Category category) {
        Book b = new Book();
        b.setIsbn(isbn);
        b.setTitle(title);
        b.setEdition("Tái bản lần 1");
        b.setPublisher("NXB Giáo dục");
        b.setPublicationYear(2022);
        b.setLanguage("Tiếng Việt");
        b.setDescription("Sách dùng cho kiểm thử tích hợp.");
        b.setAuthors(new java.util.HashSet<>(Set.of(author)));
        b.setCategories(new java.util.HashSet<>(Set.of(category)));
        return books.save(b);
    }

    private BookBatch batch(Book book, String code, int quantity, User createdBy) {
        BookBatch b = new BookBatch();
        b.setBook(book);
        b.setBatchCode(code);
        b.setQuantity(quantity);
        b.setReceivedAt(LocalDateTime.now().minusMonths(2));
        b.setSupplier("Nhà sách Fahasa");
        b.setCreatedBy(createdBy);
        return batches.save(b);
    }

    private BookCopy copy(BookBatch batch, String code, CopyStatus status) {
        BookCopy c = new BookCopy();
        c.setBatch(batch);
        c.setCopyCode(code);
        c.setStatus(status);
        c.setAcquiredAt(LocalDate.now().minusMonths(2));
        return copies.save(c);
    }

    private Loan loan(User reader, BookCopy copy, LocalDate borrowed, LocalDate due, LocalDate returned,
            LoanStatus status, User issuedBy) {
        Loan l = new Loan();
        l.setUser(reader);
        l.setCopy(copy);
        l.setBorrowedAt(borrowed.atTime(9, 0));
        l.setDueDate(due);
        l.setReturnDate(returned);
        l.setStatus(status);
        l.setIssuedBy(issuedBy);
        if (returned != null) l.setReturnedBy(issuedBy);
        return loans.save(l);
    }
}
