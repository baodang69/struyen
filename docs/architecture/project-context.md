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
- [x] Xây dựng Service gọi API và thực hiện logic Upsert vào DB.
- [x] Triển khai Cron Job tự động đồng bộ danh mục truyện.
- [x] Xử lý Image Proxy để hiển thị ảnh trên Web/App mà không bị lỗi 403 Forbidden.

### Giai đoạn 2: Viewer & UX (Hoàn thành phần Backend)
- [x] Xây dựng API trả về danh sách truyện theo phân trang và lọc theo thể loại (`F-04`).
- [x] API chi tiết truyện và danh sách chương (`F-06`).
- [x] API đọc nội dung chương (trả về danh sách URL ảnh đã qua proxy `F-06`).
- [x] API danh mục thể loại công khai (`F-07`).

### Giai đoạn 3: Social, Auth & Tương tác (Hoàn thành cốt lõi)
- [x] Hệ thống Auth & JWT (Đăng ký, Đăng nhập, Profile, Phân quyền `F-09`).
- [x] Tính năng Theo dõi (Subscription) & Lịch sử đọc (Reading History `F-10`).
- [x] Bình luận đa cấp (Comments & Replies) & Thả tim (`F-11`).
- [x] Tối ưu hóa hiệu năng JPA/N+1, Chống tràn RAM/Connection & Rate Limiting phòng vệ DoS/Spam (`F-20`).

### Lộ trình các tính năng tiếp theo (Thứ tự ưu tiên triển khai):
1. [x] **Bước 1:** `F-14` Admin Management & Content Moderation Dashboard (Hoàn thành)
2. [x] **Bước 2:** `F-20` Performance & Security Hardening (Hoàn thành)
3. [ ] **Bước 3:** `F-13` Redis Caching Optimization (Tối ưu tốc độ & giảm tải PostgreSQL)
4. [ ] **Bước 4:** `F-12` Automated User Notification System (Tích hợp Message Broker phân tán)
5. [ ] **Bước 5:** `F-08` Elasticsearch Full-Text Search & Smart Suggestions (Tối thiểu 5 truyện)
6. [ ] **Bước 6:** `PROD-SECURITY` Cloudflare Turnstile (Invisible CAPTCHA) & Email Service (Forgot Password / Account Recovery) khi triển khai Production.



