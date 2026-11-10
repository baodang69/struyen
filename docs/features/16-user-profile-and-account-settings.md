# Feature Document: User Profile and Account Settings

Tài liệu chi tiết tính năng Quản lý Hồ sơ Cá nhân, Đổi mật khẩu và Cài đặt tài khoản người dùng cho nền tảng đọc truyện Struyen.

---

## 1. Thông tin tổng quan (Overview)

- **Mã tính năng:** `USER-PROFILE-SETTINGS`
- **Tên tính năng:** Quản lý Hồ sơ Cá nhân, Cập nhật Avatar và Đổi Mật khẩu
- **Người thực hiện:** Antigravity Agent
- **Trạng thái:** Hoàn thành
- **Ngày hoàn thành:** 2026-11-10
- **Phạm vi ảnh hưởng:** `struyen_be` (API me/profile, me/change-password) và `komika-fe` (Giao diện trang cá nhân /profile với các tab Tủ truyện, Lịch sử đọc, Cài đặt tài khoản)

---

## 2. Mục tiêu và Yêu cầu (Objectives & Requirements)

1. **Quản lý thông tin cá nhân:**
   - Cung cấp API cho phép người dùng đã xác thực token JWT xem thông tin profile (`GET /api/v1/me/profile`).
   - Cập nhật ảnh đại diện cá nhân (`PUT /api/v1/me/profile`).
2. **Đổi mật khẩu bảo mật:**
   - Cung cấp API đổi mật khẩu (`POST /api/v1/me/change-password`).
   - Kiểm tra xác thực mật khẩu hiện tại bằng `PasswordEncoder.matches`.
   - Xác thực độ dài mật khẩu mới (tối thiểu 6 ký tự) và không được trùng mật khẩu cũ.
3. **Liên kết điều hướng Dropdown:**
   - Menu dropdown người dùng tại Header liên kết trực tiếp tới các phân hệ:
     - Tủ truyện (`/profile?tab=subscriptions`)
     - Lịch sử đọc (`/profile?tab=history`)
     - Cài đặt tài khoản (`/profile?tab=settings`)

---

## 3. Kiến trúc & Luồng xử lý (Architecture & Flow)

```
[User Browser (komika-fe)]
   |
   |-- 1. Click Dropdown Menu (Tủ truyện / Lịch sử đọc / Cài đặt)
   |       |-> Navigate /profile?tab={tabId}
   |
   |-- 2. Tab Tủ truyện -> GET /api/v1/me/library/subscriptions (Bearer Token)
   |       |-> Render Grid truyện, Action Đọc tiếp, Bỏ theo dõi
   |
   |-- 3. Tab Lịch sử đọc -> GET /api/v1/me/library/history (Bearer Token)
   |       |-> Render List chương đã đọc, Đọc tiếp, Xóa khỏi lịch sử
   |
   |-- 4. Tab Cài đặt -> PUT /api/v1/me/profile | POST /api/v1/me/change-password
           |-> [UserProfileController] -> [AuthService] -> [UserRepository]
```

---

## 4. Chi tiết thay đổi Code & Schema

### Backend (`struyen_be`)
- **DTOs:**
  - `UpdateProfileRequest`: `avatarUrl`
  - `ChangePasswordRequest`: `currentPassword`, `newPassword`
- **Controller:**
  - `UserProfileController`: `@RequestMapping("/api/v1/me")`
    - `GET /api/v1/me/profile`
    - `PUT /api/v1/me/profile`
    - `POST /api/v1/me/change-password`
- **Service:**
  - `AuthService`: bổ sung `updateProfile` và `changePassword`.

### Frontend (`komika-fe`)
- **Routing & Pages:**
  - `src/app/profile/page.tsx`: Route trang cá nhân tổng hợp.
  - `src/features/profile/pages/ProfilePage.tsx`: Trang quản lý với điều hướng tab mượt mà.
  - `src/features/profile/components/SubscriptionsTab.tsx`: Tủ truyện đang theo dõi.
  - `src/features/profile/components/ReadingHistoryTab.tsx`: Lịch sử các chương đã đọc.
  - `src/features/profile/components/AccountSettingsTab.tsx`: Đổi mật khẩu và cập nhật ảnh đại diện.
- **Header:**
  - `src/shared/components/Header.tsx`: Cập nhật `ACCOUNT_MENU_ITEMS` để trỏ đúng href và đóng overlay khi click.

---

## 5. Danh sách API Endpoints

| HTTP Method | Endpoint | Yêu cầu xác thực | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/me/profile` | Có (Bearer) | Lấy thông tin hồ sơ tài khoản |
| `PUT` | `/api/v1/me/profile` | Có (Bearer) | Cập nhật avatar của tài khoản |
| `POST` | `/api/v1/me/change-password` | Có (Bearer) | Đổi mật khẩu tài khoản |
| `GET` | `/api/v1/me/library/subscriptions` | Có (Bearer) | Lấy danh sách truyện đang theo dõi |
| `DELETE` | `/api/v1/me/library/subscriptions/{storyId}` | Có (Bearer) | Bỏ theo dõi truyện |
| `GET` | `/api/v1/me/library/history` | Có (Bearer) | Lấy lịch sử các chương truyện đã đọc |
| `DELETE` | `/api/v1/me/library/history/{storyId}` | Có (Bearer) | Xóa một truyện khỏi lịch sử |

---

## 6. Kiểm thử và Xác nhận (Testing & Verification)

1. Backend compilation: `./mvnw compile` hoàn thành thành công (`BUILD SUCCESS`).
2. Frontend compilation: `tsc --noEmit` hoàn thành không có bất kỳ lỗi nào.
3. Chạy kiểm tra tích hợp và điều hướng trên Header:
   - Nhấn Avatar -> Menu Dropdown hiện các lựa chọn: Tủ truyện, Lịch sử đọc, Cài đặt tài khoản.
   - Nhấn Tủ truyện -> Điều hướng tới `/profile?tab=subscriptions`, đóng dropdown.
   - Nhấn Lịch sử đọc -> Điều hướng tới `/profile?tab=history`, đóng dropdown.
   - Nhấn Cài đặt tài khoản -> Điều hướng tới `/profile?tab=settings`, đóng dropdown.
