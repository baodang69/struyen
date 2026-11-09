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
    "id": "64bc1001",
    "name": "Action",
    "slug": "action"
  },
  {
    "id": "64bc1002",
    "name": "Fantasy",
    "slug": "fantasy"
  },
  {
    "id": "64bc1003",
    "name": "Manhwa",
    "slug": "manhwa"
  }
]
```

---

## 4. Hướng dẫn kiểm thử (curl)

```bash
curl "http://localhost:8080/api/v1/public/categories"
```

