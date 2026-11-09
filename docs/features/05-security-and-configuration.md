# Feature: Spring Security & App Configuration

- **Mã tính năng:** `F-05 / SECURITY-CONFIG`
- **Trạng thái:** Hoàn thành
- **Ngày hoàn thành:** 2026-03-25
- **Tác giả / Phụ trách:** Backend Team
- **Nhánh Git:** `develop`

---

## 1. Tổng quan & Mục tiêu

- **Mục đích:** Cấu hình hạ tầng bảo mật cơ bản với Spring Security 6 và khởi tạo các Bean cốt lõi (`RestTemplate`, CORS) phục vụ giao tiếp giữa Frontend, Backend và Otruyen CDN.
- **Tính năng chính:**
  - Vô hiệu hóa CSRF (`AbstractHttpConfigurer::disable`) nhằm tối ưu cho môi trường RESTful stateless API.
  - Cho phép truy cập công khai (`permitAll()`) trong giai đoạn Phase 1 & 2 để kiểm thử API mượt mà không gặp rào cản 401 Unauthorized.
  - Cấu hình Bean `RestTemplate` hỗ trợ buffer byte array cho Image Proxy.

---

## 2. Các file mã nguồn chính

- **Security Config:**
  - `src/main/java/com/struyen/struyen/config/SecurityConfig.java`
- **RestTemplate Config:**
  - `src/main/java/com/struyen/struyen/config/RestTemplateConfig.java`
- **Main Application:**
  - `src/main/java/com/struyen/struyen/StruyenApplication.java` (kèm `@EnableScheduling`)

---

## 3. Lộ trình phát triển tiếp theo (Next Steps for Security)

- [ ] Tích hợp Spring Security với JWT (JSON Web Token).
- [ ] Phân quyền bảo vệ các endpoint `/api/v1/admin/**` (yêu cầu role `ROLE_ADMIN`).
- [ ] Rate limiting (chống spam sync hoặc proxy image).

