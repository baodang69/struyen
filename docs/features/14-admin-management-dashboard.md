# Feature: Admin Management & Content Moderation Dashboard

- **Mã tính năng:** `F-14 / ADMIN-MANAGEMENT`
- **Trạng thái:** Hoàn thành
- **Ngày hoàn thành:** 2026-11-09
- **Tác giả / Phụ trách:** Backend Team
- **Nhánh Git:** `develop`

---

## 1. Tổng quan & Mục tiêu

- **Mục đích:** Cung cấp bộ API quản trị toàn diện cho quản trị viên (Admin) để kiểm duyệt nội dung truyện, quản lý tài khoản người dùng, và đăng thông báo hệ thống.
- **Yêu cầu bảo mật:** Tất cả các endpoint quản trị đều được bảo vệ nghiêm ngặt bằng Spring Security với quyền `@PreAuthorize("hasRole('ADMIN')")`. Mặc định hệ thống tự khởi tạo tài khoản `admin / Admin@123456`.


---

## 2. Các phân hệ chức năng quản trị

### 2.1 Quản lý Nội dung Truyện (Story Moderation)
- **Ẩn / Hiện truyện (`is_active`):** Khi truyện có nội dung nhạy cảm hoặc vi phạm bản quyền, admin có thể ẩn truyện khỏi các API công khai của người dùng.
- **Gắn nhãn Đề cử (`is_featured`):** Đưa các bộ truyện hot, chất lượng cao lên khu vực Banner / Slider đề cử của trang chủ.
- **Cập nhật thủ công metadata:** Đổi tên hiển thị, tác giả, mô tả truyện.

### 2.2 Quản lý Người dùng (User Management)
- **Danh sách người dùng:** Xem danh sách độc giả trong hệ thống (kèm số lượng theo dõi, bình luận, ngày tham gia).
- **Khóa tài khoản vi phạm (`user_status: BANNED`):** Ngăn chặn tài khoản spam bình luận hoặc có hành vi phá hoại đăng nhập vào hệ thống.
- **Mở khóa tài khoản:** Chuyển trạng thái về `ACTIVE`.
- **Phân quyền (Role Assignment):** Thăng cấp người dùng lên `ROLE_ADMIN` hoặc thu hồi quyền.

### 2.3 Thông báo toàn hệ thống (System Notice)
- Đăng thông báo chung xuất hiện trên đầu trang (ví dụ: thông báo bảo trì, nâng cấp tính năng) sử dụng bảng `notice` trong cơ sở dữ liệu.

---

## 3. Danh sách API Endpoints dự kiến (Yêu cầu ROLE_ADMIN)

### 3.1 Quản lý truyện (`/api/v1/admin/stories`)
- `PATCH /api/v1/admin/stories/{id}/toggle-active`: Đổi trạng thái Ẩn/Hiện truyện
- `PATCH /api/v1/admin/stories/{id}/toggle-featured`: Đổi trạng thái Đề cử trang chủ
- `GET /api/v1/admin/stories`: Danh sách toàn bộ truyện (kể cả truyện đã bị ẩn)

### 3.2 Quản lý tài khoản (`/api/v1/admin/users`)
- `GET /api/v1/admin/users`: Danh sách người dùng có phân trang và lọc theo trạng thái
- `PATCH /api/v1/admin/users/{id}/ban`: Khóa tài khoản người dùng
- `PATCH /api/v1/admin/users/{id}/unban`: Mở khóa tài khoản

### 3.3 Thông báo hệ thống (`/api/v1/admin/notices`)
- `POST /api/v1/admin/notices`: Tạo thông báo hệ thống mới
- `GET /api/v1/admin/notices`: Danh sách thông báo
- `DELETE /api/v1/admin/notices/{id}`: Xóa thông báo

