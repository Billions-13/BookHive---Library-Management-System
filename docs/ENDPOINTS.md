# BookHive – URL, API và giao diện bàn giao Bạn 1

## Vai trò / quyền

- `STUDENT`, `LECTURER`: trang cá nhân, tự mượn sách (khi còn và không có queue), đặt trước khi hết sách.
- `LIBRARIAN`: thêm/sửa sách, nhập lô, quản lý bản sao, phát hành/trả sách, phạt, CSV.
- `ADMIN`: mọi chức năng Librarian + quản lý tài khoản. Spring Security kiểm tra ở **backend**, không phụ thuộc CSS/JS.
- Các form có POST phải giữ `th:action` + method POST để Spring Security xử lý CSRF.

## URLs và method

| URL | Method | Quyền | Công việc |
|---|---|---|---|
| `/` | GET | Public | Home |
| `/books` | GET | Public | Catalogue: 7 filter, sorting và paging 10/20/50/100 |
| `/books/{id}` | GET | Public | Chi tiết, tồn kho, nút borrow/reserve |
| `/register`, `/login` | GET/POST | Public | Đăng ký, đăng nhập |
| `/logout` | POST | Signed in | Đăng xuất |
| `/my/profile`, `/my/password` | GET/POST | Signed in | Hồ sơ, đổi mật khẩu |
| `/my/loans` | GET | Signed in | Sách đang mượn/lịch sử |
| `/my/borrow/{bookId}` | POST | Student/Lecturer (logic service) | Mượn một bản sao khả dụng |
| `/my/reservations` | GET | Signed in | Hàng đợi của mình |
| `/my/reservations/{bookId}` | POST | Student/Lecturer (logic service) | Đặt trước khi hết |
| `/my/reservations/{id}/cancel` | POST | Owner only | Hủy đặt |
| `/my/fines` | GET | Signed in | Tiền phạt của mình |
| `/librarian/books/new` | GET/POST | Staff | Tạo đầu sách |
| `/librarian/books/{id}/edit` | GET/POST | Staff | Chỉnh sửa đầu sách |
| `/librarian/books/{id}/archive` | POST | Staff | Xóa mềm, cấm khi còn giao dịch mở |
| `/librarian/books/{id}/inventory` | GET | Staff | Bảng lô/bản sao, trạng thái |
| `/librarian/books/{id}/batches` | POST | Staff | Nhập thêm số lượng, mã bản sao riêng |
| `/librarian/books/{id}/batches/{batchId}/discard` | POST | Staff | Thanh lý lô (chỉ khi không đang mượn/giữ) |
| `/librarian/books/{id}/copies/{copyId}/status` | POST | Staff | Đánh dấu AVAILABLE/DAMAGED/LOST/DISCARDED |
| `/librarian/loans` | GET/POST | Staff | Xem, phát hành phiếu mượn |
| `/librarian/loans/{id}/return` | POST | Staff | Trả và tính phạt |
| `/librarian/fines` | GET | Staff | Danh sách phạt |
| `/librarian/fines/{id}/resolve` | POST | Staff | PAID hoặc WAIVED |
| `/librarian/authors`, `/librarian/categories` | GET | Staff | 5 bộ lọc, sort và paging; `/new`, `/{id}/edit`, `/{id}/archive` tương ứng |
| `/librarian/catalog/csv` | GET | Staff | CSV menu |
| `/librarian/catalog/import` | POST | Staff | Upload CSV, rollback toàn file nếu lỗi |
| `/librarian/catalog/export` | GET | Staff | Xuất CSV (giảm rủi ro formula injection) |
| `/admin/users` | GET | Admin | List users, 5 bộ lọc, sort và paging |
| `/admin/users/new`, `/admin/users/{id}/edit` | GET/POST | Admin | Tạo, sửa, đổi role, reset password |
| `/admin/users/{id}/toggle` | POST | Admin | Khóa/mở account |
| `/api/stats` | GET | Staff | JSON thống kê cho Chart.js |

API JSON thống kê: `{"totalTitles":31,"availableCopies":0,"borrowedCopies":0,"overdueLoans":0}`. Số là **ví dụ định dạng**, không phải kết quả live; dữ liệu thực lấy từ MySQL.

## Quy tắc tồn kho

Một `book_batches` có `quantity` vật lý. Mỗi lần nhập thêm tạo `quantity` `book_copies` với `copy_code` dạng `COPY010191`, **không tái sử dụng mã của sách cũ**. Khi hết `AVAILABLE`, trang chi tiết hiển thị hết sách, cho đặt trước. Đổi bản khác ISBN: tạo đầu sách mới trước, sau đó nhập lô mới và thanh lý sách cũ khi không còn phiếu đang mở.

Mã mới lấy từ `id` tự tăng do MySQL cấp (ví dụ `id=10191` → `COPY010191`), đảm bảo duy nhất kể cả nhập đồng thời. `COPY001`, `COPY002` trong phân công chỉ là ví dụ minh họa định dạng.

## Bạn 2 / Bạn 3 nên giữ nguyên

- `th:field`, `name`, `th:action` của `<form>`; các URL phía trên; trường `id` khi dùng label.
- Bạn 2 chỉ thêm CSS/responsive, không sửa cấu trúc Java/SQL hoặc bỏ CSRF token.
- Bạn 3 chỉ thêm JS Fetch/DOM, dashboard Chart.js, tích hợp CSV/email và deploy; không tạo backend mượn/trả thứ hai.
- Các URL trả HTML không phải JSON API (ngoại trừ `/api/stats` và file CSV). Muốn AJAX toàn diện thì cần hai bên thống nhất contract trước.
