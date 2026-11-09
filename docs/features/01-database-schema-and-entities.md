# Feature: Thiết lập Database Schema & Entities

- **Mã tính năng:** `F-01 / DB-CORE`
- **Trạng thái:** Hoàn thành
- **Ngày hoàn thành:** 2026-03-25
- **Tác giả / Phụ trách:** Backend Team
- **Nhánh Git:** `develop`

---

## 1. Tổng quan & Mục tiêu

- **Mục đích:** Xây dựng cấu trúc lưu trữ cơ sở dữ liệu trên PostgreSQL để hỗ trợ hệ thống Webtoon Aggregator: quản lý truyện (`story`), thể loại (`category`), danh sách chương (`chapter`) với mảng ảnh lưu trữ dạng `JSONB`.
- **Điểm nổi bật:**
  - Tối ưu hóa đọc dữ liệu chương qua kiểu dữ liệu PostgreSQL `JSONB` (`images_json`), tránh tạo hàng triệu row rời rạc cho từng ảnh.
  - Sử dụng Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` trên Spring Boot 3.
  - Indexing tối ưu cho việc tìm kiếm theo `slug`, thời gian cập nhật `updated_at`, và trạng thái kích hoạt `is_active`.

---

## 2. Kiến trúc & Quan hệ dữ liệu (ER Diagram)

```mermaid
erDiagram
    STORY ||--o{ STORY_CATEGORY : has
    CATEGORY ||--o{ STORY_CATEGORY : belongs_to
    STORY ||--o{ CHAPTER : contains

    STORY {
        string id PK
        string slug UK
        string name
        string status
        string thumb_url
        string cdn_domain
        text description
        string author
        bigint view_count
        boolean is_active
        boolean is_featured
        timestamp origin_updated_at
        timestamp created_at
        timestamp updated_at
        timestamp last_synced_at
        timestamp last_chapter_sync_at
    }

    CATEGORY {
        string id PK
        string name
        string slug UK
    }

    CHAPTER {
        string id PK
        string story_id FK
        string chapter_name
        string chapter_title
        string chapter_api_data UK
        jsonb images_json
        boolean is_content_synced
        timestamp created_at
        timestamp updated_at
    }
```

---

## 3. Các file mã nguồn chính

- **Entities:**
  - `src/main/java/com/struyen/struyen/entities/Story.java`
  - `src/main/java/com/struyen/struyen/entities/Category.java`
  - `src/main/java/com/struyen/struyen/entities/Chapter.java`
- **Repositories:**
  - `src/main/java/com/struyen/struyen/repositories/StoryRepository.java`
  - `src/main/java/com/struyen/struyen/repositories/CategoryRepository.java`
  - `src/main/java/com/struyen/struyen/repositories/ChapterRepository.java`
- **Database DDL Reference:**
  - `docs/database/schema.sql`

---

## 4. Xác minh & Kiểm thử

- Chạy `.\mvnw.cmd compile` đảm bảo các Entity JPA và quan hệ mapping hợp lệ.
- Kiểm tra cấu hình kết nối database trong `src/main/resources/application.properties` với PostgreSQL.

