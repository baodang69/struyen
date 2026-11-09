# Project Context: Webtoon Clone (Aggregator Model)

## 1. Tổng quan dự án (Project Overview)
- **Tên dự án:** Struyen.
- **Mô hình:** Aggregator (Tổng hợp nội dung).
- **Nguồn dữ liệu:** Dữ liệu từ bên thứ ba thông qua Otruyen API.
- **Mục tiêu:** Xây dựng nền tảng đọc truyện tối ưu UX/UI, tốc độ tải ảnh nhanh, hỗ trợ cộng đồng tương tác mạnh mẽ và quản lý nội dung thông minh.

---

## 2. Các phân hệ chức năng (Core Features)

### A. Phân hệ Người dùng (Client)
- **Trình đọc truyện:** Hỗ trợ cuộn dọc, Lazy Loading ảnh, tự động lưu lịch sử đọc.
- **Thư viện cá nhân:** Theo dõi truyện (Subscription), danh sách yêu thích, đồng bộ lịch sử đọc trên nhiều thiết bị.
- **Tương tác:** Bình luận đa cấp (Reply), Like/Vote bình luận, đánh giá truyện.
- **Hệ thống thông báo:** Nhận thông báo (Web Push/In-app) khi truyện đang theo dõi có chương mới.

### B. Phân hệ Quản trị (Admin)
- **Content Moderation:** Ẩn/Hiện truyện (`is_active`), gắn nhãn truyện nổi bật (`is_featured`).
- **User Management:** Quản lý tài khoản, khóa người dùng vi phạm, phân quyền (Admin/Member).
- **System Notice:** Tạo thông báo hệ thống cho toàn bộ người dùng.

### C. Phân hệ Hệ thống (Backend)
- **Cron Job Sync:** Tự động gọi Otruyen API định kỳ (30-60p) để cập nhật truyện mới.
- **Image Proxy:** Xử lý hiển thị ảnh để vượt rào cản Hotlink Protection từ nguồn API.
- **Caching:** Lưu trữ Metadata và JSON danh sách ảnh để giảm tải cho API gốc và tăng tốc độ phản hồi.

---

## 3. Kiến trúc Cơ sở dữ liệu (Database Schema)
- **Hệ quản trị cơ sở dữ liệu:** PostgreSQL.
- **Chiến lược lưu trữ ảnh:** Sử dụng kiểu dữ liệu `JSONB` trong bảng `chapter` để tối ưu performance.
- Xem chi tiết tại: [`docs/database/schema.sql`](file:///d:/Code/struyen_be/docs/database/schema.sql).

---

## 4. Tài liệu API bên thứ ba (External API Reference)
Toàn bộ dữ liệu thô được khai thác từ Otruyen:
- **Trang chủ Document:** `https://otruyenapi.com`
- **Danh sách truyện mới:** `https://otruyenapi.com/v1/api/danh-sach/truyen-moi`
- **Chi tiết truyện:** `https://otruyenapi.com/v1/api/truyen/{slug}`
- **Danh sách chương:** Nằm trong API chi tiết truyện.
- **Chi tiết chương (Ảnh):** `https://sv1.otruyencdn.com/v1/api/chapter/{chapter_api_data}`

---

## 5. Kế hoạch phát triển (Roadmap)

### Giai đoạn 1: Infrastructure & Sync (Hoàn thành)
- [x] Khởi tạo Project Spring Boot 3 & Java 21.
- [x] Thiết lập kết nối PostgreSQL.
- [x] Xây dựng Service gọi API và thực hiện logic Upsert (Update or Insert) vào DB.
- [x] Triển khai Cron Job tự động đồng bộ danh mục truyện.
- [x] Xử lý Image Proxy để hiển thị ảnh trên Web/App mà không bị lỗi 403 Forbidden.

### Giai đoạn 2: Viewer & UX (Đang triển khai)
- [x] Xây dựng API trả về danh sách truyện theo phân trang và lọc theo thể loại.
- [ ] API chi tiết truyện và danh sách chương.
- [ ] API đọc nội dung chương (trả về danh sách URL ảnh đã qua proxy).
- [ ] Xây dựng giao diện Reader (Frontend).

### Giai đoạn 3: Social & Notification
- [ ] Hệ thống Auth (JWT).
- [ ] Tính năng Theo dõi, Lịch sử đọc và Bình luận.
- [ ] Hệ thống thông báo tự động khi có chương mới.

### Giai đoạn 4: Optimization & Admin
- [ ] Tối ưu Cache (Redis).
- [ ] Hoàn thiện Dashboard cho Admin quản lý ẩn/hiện nội dung.

