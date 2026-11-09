# Feature: Comment, Reply & Like System

- **Mã tính năng:** `F-11 / COMMENT-SYSTEM`
- **Trạng thái:** Hoàn thành
- **Ngày hoàn thành:** 2026-11-09
- **Tác giả / Phụ trách:** Backend Team
- **Nhánh Git:** `develop`

---

## 1. Tổng quan & Mục tiêu

- **Mục đích:** Xây dựng hệ thống tương tác cộng đồng mạnh mẽ cho độc giả:
  1. **Bình luận đa cấp (Comments & Replies)**: Cho phép người dùng bình luận ở trang chi tiết truyện hoặc trong từng chương cụ thể. Hỗ trợ trả lời (Reply) bình luận của người khác.
  2. **Thả tim / Thích (Like)**: Người đọc có thể like bình luận yêu thích.
  3. **Xóa mềm (Soft Delete)**: Đảm bảo bảo toàn chuỗi thảo luận khi một bình luận cha bị xóa (chuyển trạng thái `is_deleted = true`).

---

## 2. Kiến trúc & Phân quyền

- **Người đọc vãng lai (Public / Unauthenticated):**
  - Được xem danh sách bình luận trên trang truyện (`GET /api/v1/public/comments/story/{storyId}`).
  - Được xem bình luận trong chương (`GET /api/v1/public/comments/chapter/{chapterId}`).
  - Được xem toàn bộ reply của một bình luận (`GET /api/v1/public/comments/{commentId}/replies`).
  - Được like bình luận (`POST /api/v1/public/comments/{commentId}/like`).
- **Người dùng đã đăng nhập (Authenticated with JWT):**
  - Viết bình luận mới hoặc gửi reply (`POST /api/v1/me/comments`).
  - Xóa bình luận của chính mình (`DELETE /api/v1/me/comments/{commentId}`).
- **Quản trị viên (ROLE_ADMIN):**
  - Có quyền xóa bất kỳ bình luận nào vi phạm chính sách cộng đồng.

---

## 3. Các file mã nguồn chính

- **Entity & Repository:**
  - `src/main/java/com/struyen/struyen/entities/Comment.java`
  - `src/main/java/com/struyen/struyen/repositories/CommentRepository.java`
- **Service & Controllers:**
  - `src/main/java/com/struyen/struyen/services/CommentService.java`
  - `src/main/java/com/struyen/struyen/controllers/PublicCommentController.java`
  - `src/main/java/com/struyen/struyen/controllers/UserCommentController.java`
- **DTOs:**
  - `src/main/java/com/struyen/struyen/dtos/CreateCommentRequest.java`
  - `src/main/java/com/struyen/struyen/dtos/CommentDto.java`

---

## 4. Danh sách API Endpoints

### 4.1 Xem bình luận công khai (Public)
- **Lấy bình luận ở trang truyện:** `GET /api/v1/public/comments/story/{storyId}?page=1&size=10`
- **Lấy bình luận trong chương:** `GET /api/v1/public/comments/chapter/{chapterId}?page=1&size=10`
- **Lấy toàn bộ reply của một comment:** `GET /api/v1/public/comments/{commentId}/replies`
- **Thả tim bình luận:** `POST /api/v1/public/comments/{commentId}/like`

### 4.2 Thao tác bình luận của người dùng (Authenticated)
- **Đăng bình luận / Reply:** `POST /api/v1/me/comments`
  - Headers: `Authorization: Bearer <token>`
  - Request Body:
    ```json
    {
      "storyId": "solo-leveling",
      "chapterId": "chap-01",
      "parentId": null,
      "content": "Truyện này đỉnh quá mọi người ơi!"
    }
    ```
- **Xóa bình luận:** `DELETE /api/v1/me/comments/{commentId}`
  - Headers: `Authorization: Bearer <token>`

---

## 5. Hướng dẫn kiểm thử (curl)

```bash
# 1. Đăng bình luận mới
curl -X POST "http://localhost:8080/api/v1/me/comments" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"storyId": "solo-leveling", "content": "Hay tuyet voi!"}'

# 2. Like bình luận
curl -X POST "http://localhost:8080/api/v1/public/comments/1/like"

# 3. Xem danh sách bình luận
curl "http://localhost:8080/api/v1/public/comments/story/solo-leveling"
```
