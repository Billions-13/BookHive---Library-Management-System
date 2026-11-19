# BookHive – Database Design (Phần II)

Cơ sở: phân công Final Project – University Library Management System, trang 2–4 và checklist trang 8–9.

- MySQL: 8.0.16 trở lên; charset UTF8MB4; ENGINE InnoDB.
- **14 bảng**: roles, users, books, book_copies, book_batches, authors, categories, loans, reservations, fines, notifications, audit_logs, book_authors, book_categories.
- **2 junction tables**: book_authors, book_categories (PK kép).
- **Sách thật, dữ liệu giao dịch giả lập**: 31 đầu sách IT được tổng hợp từ nguồn phát hành; không tuyên bố thư viện sở hữu sách thật hay có người dùng thật.
- **Thống kê dữ liệu mẫu**: 615 dòng trong 14 bảng (xem bảng dưới).

| Bảng | Số bản ghi mẫu |
|---|---:|
| `roles` | 4 |
| `users` | 28 |
| `authors` | 53 |
| `categories` | 14 |
| `books` | 31 |
| `book_authors` | 55 |
| `book_categories` | 55 |
| `book_batches` | 38 |
| `book_copies` | 190 |
| `loans` | 48 |
| `reservations` | 19 |
| `fines` | 12 |
| `notifications` | 22 |
| `audit_logs` | 46 |

## Chuẩn hóa dữ liệu (3NF)

- `books` lưu metadata một đầu sách theo ISBN; `book_copies` lưu từng cuốn vật lý theo mã độc lập; `book_batches` lưu từng lần nhập và người nhập. **Để đúng 3NF, `book_copies` không lưu `book_id` trùng với `book_batches.book_id`; tra cuốn → lô → đầu sách.**
- `authors` và `categories` không lặp trực tiếp trong `books`: liên kết n–n qua `book_authors`, `book_categories`.
- `loans` chỉ lưu PK của người mượn, bản sao và thời hạn; `fines` trỏ đến khoản vay, không chép lại thông tin sách/người.
- Số lượng khả dụng **không lưu trong books**, được tính từ `book_copies.status`; vì vậy không tạo trường đếm tồn kho dễ sai lệch.
- `roles` tách riêng chính sách số lượng và thời gian mượn, áp dụng 1-n qua `users.role_id`.

## Quy tắc nghiệp vụ/khóa

- `books.isbn`, `users.email`, `book_copies.copy_code`, `book_batches.batch_code` UNIQUE.
- `loans.active_copy_id` generated+UNIQUE: một cuốn không có hai loan `ACTIVE` đồng thời.
- `book_batches.quantity > 0`; statuses/CHECK và tiền phạt không âm.
- Khi viết Java `BookCopy`, chỉ map `@ManyToOne BookBatch batch` và lấy đầu sách bằng `copy.getBatch().getBook()`. Repository tìm sách qua `batch.book.id`, không dùng cột `book_copies.book_id` của ví dụ cũ.
- FK kiểm soát sự tồn tại; transaction/locking và kiểm tra logic trong Java vẫn cần để ngăn tranh chấp đặt trước, giới hạn mượn, thanh lý cuốn đang mượn.
- `DISCARDED` là trạng thái thanh lý; không xóa lịch sử lô và phiếu mượn.
- 5 đầu sách không còn bản `AVAILABLE` (trong đó một đầu sách chưa nhập bản sao): hỗ trợ kiểm thử thông báo hết sách.

## Hướng dẫn chạy MySQL Workbench

1. Giữ nguyên các file trong folder `database/` tại thư mục gốc project (cùng cấp `pom.xml`).
2. Kết nối MySQL 8 bằng tài khoản đủ quyền tạo bảng. Trước khi chạy cần **đảm bảo database development trống hoặc đã backup**. Các SQL sử dụng ID mẫu cố định, không dành cho môi trường đã có dữ liệu thực.
3. Mở `schema.sql`, bấm biểu tượng tia sét **Execute**.
4. Mở `seed-data.sql`, bấm **Execute**. **Chỉ chạy một lần** trên DB trống.
5. Mở `verify.sql`, chạy và kiểm tra 14 bảng, trên 50 cột, nhiều hơn 100 dòng dữ liệu, và các truy vấn kiểm tra lỗi đều rỗng.
6. Cấu hình Spring Boot với `DB_URL=jdbc:mysql://localhost:3306/bookhive_db`, `DB_USERNAME` và `DB_PASSWORD` của MySQL. Khi sử dụng schema SQL làm nguồn chuẩn, chọn `spring.jpa.hibernate.ddl-auto=validate` thay cho `update`.

## Tài khoản để demo LOCAL (KHÔNG dùng trên Internet)

- Admin: `admin@bookhive.example.test`
- Librarian: `librarian@bookhive.example.test`
- Lecturer: `lecturer@bookhive.example.test`
- Student: `student@bookhive.example.test`
- Mật khẩu **chỉ dành cho dữ liệu mẫu development**: `BookHiveDemo!2026` (đã hash BCrypt trong SQL).
- Email thuộc `example.test` là địa chỉ mô phỏng, **không phải email gửi thật**; email reminders phải dùng hộp thư test riêng nếu muốn gửi.
- Trước khi deploy Railway: **không đưa những tài khoản/mật khẩu demo này lên production**. Hãy seed production riêng, đổi mật khẩu và dùng `ADMIN_EMAIL`/`ADMIN_PASSWORD` đặt qua environment variables.

## Nguồn sách và dữ liệu giả lập

- `catalog_sources.csv`: danh sách đường dẫn đối chiếu tựa sách, ISBN, tác giả và phiên bản.
- Năm xuất bản lấy từ nguồn catalog khi có; tùy ebook/print, các nguồn có thể công bố năm/ISBN khác nhau cho cùng một phiên bản. Cần đối chiếu khi ghi nhận mã sách vật lý thực tế.
- Mọi thông tin `users`, `book_batches`, `book_copies`, `loans`, `reservations`, `fines`, `notifications`, `audit_logs` là dữ liệu demo có chủ đích.
- `cover_url` giữ NULL vì chưa có URL ảnh bìa ổn định/có quyền sử dụng được xác minh; bạn 2 có thể bổ sung hình phù hợp ở giai đoạn UI.
- Vì các script tạo bảng trực tiếp, tránh đồng thời bật cơ chế tự chạy `schema.sql` và `ddl-auto=create` để không xung đột schema.

## File bàn giao

- `schema.sql`: đầy đủ CREATE TABLE, PK, FK, UNIQUE, CHECK, INDEX.
- `seed-data.sql`: sách IT thực tế + dữ liệu mẫu nghiệp vụ.
- `erd.mmd`: ERD Mermaid 14 bảng.
- `verify.sql`: truy vấn kiểm tra tổng hợp và các ràng buộc liên kết.
- `catalog_sources.csv`: provenance của danh mục sách.
