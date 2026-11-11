# Feature Tracking Registry - Struyen Backend

Bảng danh sách tổng hợp tất cả các tính năng (features) đã hoàn thành và đang phát triển trong dự án `struyen_be`. Mỗi tính năng có một file chi tiết riêng tương ứng trong thư mục `docs/features/`.

---

## 1. Danh sách tính năng (Features Registry)

| ID | Mã tính năng | Tên tính năng | Trạng thái | Ngày hoàn thành | Tài liệu chi tiết |
|---|---|---|---|---|---|
| F-01 | `DB-CORE` | Thiết lập PostgreSQL Schema & Entities (Story, Category, Chapter with JSONB) | Hoàn thành | 2026-03-25 | [01-database-schema-and-entities.md](./01-database-schema-and-entities.md) |
| F-02 | `SYNC-OTRUYEN` | Đồng bộ dữ liệu truyện từ Otruyen API & Cron Job định kỳ | Hoàn thành | 2026-03-26 | [02-otruyen-sync-service.md](./02-otruyen-sync-service.md) |
| F-03 | `IMAGE-PROXY` | Image Proxy Controller & Service (Bypass Hotlink Otruyen CDN) | Hoàn thành | 2026-03-25 | [03-image-proxy-service.md](./03-image-proxy-service.md) |
| F-04 | `PUBLIC-STORY-API` | Public Story Controller & Service (Danh sách truyện, phân trang, lọc theo thể loại) | Hoàn thành | 2026-05-06 | [04-public-story-api.md](./04-public-story-api.md) |
| F-05 | `SECURITY-CONFIG` | Cấu hình Spring Security, CORS & RestTemplate | Hoàn thành | 2026-03-25 | [05-security-and-configuration.md](./05-security-and-configuration.md) |
| F-06 | `STORY-DETAIL-READER` | API Chi tiết truyện & Nội dung đọc chương (Proxy ảnh, Prev/Next) | Hoàn thành | 2026-11-09 | [06-story-detail-and-reader-api.md](./06-story-detail-and-reader-api.md) |
| F-07 | `PUBLIC-CATEGORY-API` | API Danh mục thể loại truyện công khai | Hoàn thành | 2026-11-09 | [07-public-category-api.md](./07-public-category-api.md) |
| F-08 | `ELASTICSEARCH-SEARCH` | Tìm kiếm Elasticsearch & Auto-suggestion tối thiểu 5 truyện | Dự thảo / Planning | - | [08-elasticsearch-search-and-suggestion.md](./08-elasticsearch-search-and-suggestion.md) |
| F-09 | `USER-AUTH-JWT` | Hệ thống Định danh, Đăng ký, Đăng nhập & Xác thực JWT | Hoàn thành | 2026-11-09 | [09-user-authentication-jwt.md](./09-user-authentication-jwt.md) |
| F-10 | `PERSONAL-LIBRARY` | Thư viện cá nhân: Theo dõi truyện & Lịch sử đọc truyện | Hoàn thành | 2026-11-09 | [10-personal-library.md](./10-personal-library.md) |
| F-11 | `COMMENT-SYSTEM` | Hệ thống Bình luận đa cấp, Trả lời (Reply) & Thả tim (Like) | Hoàn thành | 2026-11-09 | [11-comment-and-reply-system.md](./11-comment-and-reply-system.md) |
| F-14 | `ADMIN-MANAGEMENT` | Bảng điều khiển quản trị: Kiểm duyệt truyện, Khóa User & Notice | Hoàn thành | 2026-11-09 | [14-admin-management-dashboard.md](./14-admin-management-dashboard.md) |
| F-15 | `SYNC-MANGADEX` | Tích hợp nguồn truyện MangaDex API (Multi-Source Provider) & On-demand Loading | Hoàn thành | 2026-11-09 | [15-mangadex-multi-source-sync.md](./15-mangadex-multi-source-sync.md) |
| F-16 | `USER-PROFILE-SETTINGS` | Quản lý Hồ sơ Cá nhân, Cập nhật Avatar & Đổi Mật khẩu | Hoàn thành | 2026-11-10 | [16-user-profile-and-account-settings.md](./16-user-profile-and-account-settings.md) |
| F-17 | `SERVER-SIDE-PAGINATION` | Phân trang, Lọc thể loại, Sắp xếp & Tìm kiếm động phía Server-side | Hoàn thành | 2026-11-10 | [17-server-side-pagination-and-filtering.md](./17-server-side-pagination-and-filtering.md) |
| F-18 | `FEATURED-STORIES-NAV` | Quản lý Đề cử Trang chủ (Bump to Top) & Tinh chỉnh điều hướng Thể loại/Mới cập nhật | Hoàn thành | 2026-11-11 | [18-featured-stories-and-navigation-refinement.md](./18-featured-stories-and-navigation-refinement.md) |
| F-13 | `REDIS-CACHE` | Tối ưu hóa Cache Redis cho trang chủ, chi tiết truyện & danh mục | Kế hoạch (Tiếp theo) | - | [13-redis-caching-optimization.md](./13-redis-caching-optimization.md) |
| F-12 | `NOTIFICATION-SYSTEM` | Hệ thống Thông báo tự động kèm Message Broker phân tán | Kế hoạch | - | [12-notification-system.md](./12-notification-system.md) |
| F-08 | `ELASTICSEARCH-SEARCH` | Tìm kiếm Elasticsearch & Auto-suggestion tối thiểu 5 truyện | Kế hoạch | - | [08-elasticsearch-search-and-suggestion.md](./08-elasticsearch-search-and-suggestion.md) |








---

## 2. Quy tắc ghi nhận tính năng mới

Khi tạo một tính năng mới hoặc nâng cấp một tính năng lớn:
1. Sao chép mẫu từ [`FEATURE_TEMPLATE.md`](./FEATURE_TEMPLATE.md).
2. Tạo file mới theo định dạng `docs/features/<STT>-<ten-tinh-nang>.md` (ví dụ: `06-user-auth-jwt.md`).
3. Điền đầy đủ thông tin: Mục tiêu, Kiến trúc/Flow, Thay đổi Database/Code, Danh sách API, Cách kiểm thử (Test).
4. Cập nhật bảng tổng hợp trên trong file `README.md` này.

