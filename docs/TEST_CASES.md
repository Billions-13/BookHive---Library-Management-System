# BookHive – 15 tình huống kiểm thử nghiệp vụ

Tất cả trường hợp dưới đây cần kiểm tra trên **Windows + Spring Boot + MySQL thật**, không thể suy ra là PASS chỉ vì code đã viết. Thực hiện trên database development; backup trước khi thử ghi.

| # | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| 1 | Mở `/` và `/books` khi chưa login | Truy cập được |
| 2 | Tìm kiếm theo title, ISBN, author, category, publisher, language, year | Kết quả đúng, không duplicate khi nhiều tác giả |
| 3 | Sắp xếp + các page size 10/20/50/100 và Next/Previous | Phân trang đúng |
| 4 | Đăng ký email mới và đăng nhập | Lưu BCrypt, role STUDENT |
| 5 | Đăng ký trùng email hoặc sai password | Hiện lỗi hợp lý |
| 6 | Đăng nhập sai 5 lần rồi thử đăng nhập | Khóa tạm 15 phút; đăng nhập đúng sau hạn thì reset |
| 7 | Student mở `/admin/users` hoặc `/librarian/loans` | Bị chặn tại server (403) |
| 8 | Librarian tạo/sửa/archive đầu sách; archive đang có active loan | Cập nhật đúng; từ chối archive khi còn giao dịch |
| 9 | Nhập lô 3 bản sao mới | Có đúng 3 mã COPY khác nhau, batch history còn nguyên |
| 10 | Đánh dấu hư/thanh lý sách đang mượn | Server từ chối; không mất lịch sử |
| 11 | Student mượn đủ hạn mức rồi mượn thêm | Bị từ chối; không sinh 2 phiếu ACTIVE cho 1 copy |
| 12 | Mượn đến khi hết bản AVAILABLE | Hiện OUT OF STOCK; không cho mượn tiếp |
| 13 | Đặt trước khi hết; trả sách hoặc nhập lô mới | Hàng đợi oldest-first; chuyển READY, copy RESERVED |
| 14 | Trả quá hạn + resolve fine | Trả xong status RETURNED, tiền phạt 2.000 VNĐ/ngày, PAID/WAIVED |
| 15 | Import CSV hợp lệ / CSV trùng ISBN, export, `/api/stats` | Import thành công hoặc rollback khi lỗi; CSV/JSON lấy dữ liệu MySQL |

**Kiểm tra database** bằng `database/verify.sql`: count bảng/cột, mã bản sao trùng, phiếu mượn đang hoạt động trùng, phiếu READY không giữ copy, batch mismatch. Kiểm tra log backend nếu có lỗi, chụp ảnh màn hình kết quả cho report.

**Chưa kiểm thử trong môi trường này:** Maven build + MySQL end-to-end vì môi trường kiểm tra không có Maven tải phụ thuộc và MySQL. Các tình huống trên là checklist yêu cầu chạy thực tế trước khi bàn giao.
