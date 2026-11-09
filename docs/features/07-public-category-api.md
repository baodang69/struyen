# Feature: Public Category API

- **Mã tính năng:** `F-07 / PUBLIC-CATEGORY-API`
- **Trạng thái:** Hoàn thành
- **Ngày hoàn thành:** 2026-11-09
- **Tác giả / Phụ trách:** Backend Team
- **Nhánh Git:** `develop`

---

## 1. Tổng quan & Mục tiêu

- **Mục đích:** Cung cấp API công khai trả về danh sách tất cả các thể loại truyện tranh có trong hệ thống (`Action`, `Fantasy`, `Manhwa`,...), sắp xếp theo thứ tự bảng chữ cái alphabet (`name ASC`).
- **Ứng dụng:** Phục vụ Frontend render thanh điều hướng (Navbar), danh mục thể loại (Category Menu) và danh sách tùy chọn cho bộ lọc tìm kiếm.

---

## 2. Các file mã nguồn chính

- **Controller:**
  - `src/main/java/com/struyen/struyen/controllers/PublicCategoryController.java`
- **Service:**
  - `src/main/java/com/struyen/struyen/services/PublicStoryService.java` (`getAllCategories`)
- **Repository:**
  - `src/main/java/com/struyen/struyen/repositories/CategoryRepository.java` (`findAllByOrderByNameAsc`)
- **DTO:**
  - `src/main/java/com/struyen/struyen/dtos/CategoryDto.java`

---

## 3. Danh sách API Endpoints

### 3.1 Lấy toàn bộ danh sách thể loại
- **Method:** `GET`
- **URL:** `/api/v1/public/categories`
- **Response mẫu (200 OK):**
```json
[
  {
    "id": "cat-action",
    "name": "Hành Động",
    "slug": "action"
  },
  {
    "id": "cat-comedy",
    "name": "Hài Hước",
    "slug": "comedy"
  }
]
```

### 3.2 Lọc danh sách truyện theo thể loại linh hoạt
- **Method:** `GET`
- **URL:** `/api/v1/public/stories?category={category}`
- **Hỗ trợ định dạng tham số `category` đa dạng:**
  - Tên tiếng Việt: `?category=Hành Động` hoặc `?category=hành động`
  - Slug tiếng Anh: `?category=action`
  - Slug tiếng Việt không dấu: `?category=hanh-dong`
  - ID thể loại: `?category=cat-action`
  - Lấy tất cả: không truyền hoặc truyền `?category=Tất cả`
- **Response mẫu (200 OK):**
  Trả về `PageResponse<StoryListDto>` trong đó mỗi item chứa trường `categories` danh sách thể loại đầy đủ.

---

## 4. Hướng dẫn kiểm thử (curl / PowerShell)

```bash
# Lấy danh mục thể loại
curl "http://localhost:8080/api/v1/public/categories"

# Lọc truyện theo tên tiếng Việt
curl "http://localhost:8080/api/v1/public/stories?category=Hành%20Động"

# Lọc truyện theo slug tiếng Anh
curl "http://localhost:8080/api/v1/public/stories?category=action"

# Lọc truyện theo slug không dấu
curl "http://localhost:8080/api/v1/public/stories?category=hanh-dong"
```

