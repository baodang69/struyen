# Feature F-23: Google OAuth2 Authentication (Xac thuc dang nhap Google)

Tich hop xac thuc dang nhap va dang ky tu dong bang tai khoan Google thong qua Google Identity Services (GIS) va co che Dual-Token JWT tren he thong Struyen.

---

## 1. Muc tieu & Yeu cau nghiep vu

1. **Trai nghiem dang nhap khong can mat khau:**
   - Cho phep doc gia dang nhap nhanh chong bang tai khoan Google thong qua Google Identity Services (GIS) ma khong can dien form dang ky truyen thong.
   - Tu dong lay email, ho ten va avatar cua nguoi dung tu Google.
2. **Tu dong tao tai khoan & Phong ve Account Pre-Hijacking:**
   - Neu email chua ton tai trong he thong: Tu dong tao tai khoan moi voi role mac dinh `ROLE_MEMBER`, username duoc tao duy nhat tu ho ten/email va mat khau ngau nhien duoc ma hoa BCrypt.
   - Neu email da ton tai duoi hinh thuc tai khoan thuong (`authProvider = LOCAL`): Tu choi tu dong gop tai khoan (Account Pre-Hijacking Protection), tra ve loi `409 Conflict` yeu cau dang nhap bang mat khau.
   - Neu tai khoan da tung dang nhap Google: Tu dong xac thuc va cap nhat avatar neu co thay doi.
3. **Tuong thich hoan toan voi co che Dual-Token hien tai:**
   - Cap Access Token (JWT 15 phut) tra ve trong response JSON.
   - Cap Refresh Token (7 ngay) luu trong Redis kem cookie HttpOnly an toan.
4. **Bao ve he thong:**
   - Kiem tra tinh hop le cua token thong qua Google Tokeninfo API (`https://oauth2.googleapis.com/tokeninfo`).
   - Kiem tra `email_verified == true`.
   - Chan tai khoan bi khoa (`userStatus == BANNED`) voi ma loi `403 Forbidden`.

---

## 2. Kien truc & Luong xu ly (Flow)

```
[ Frontend: Next.js (komika-fe) ]
   |
   | 1. Nguoi dung chon "Tiep tuc voi Google"
   | 2. Google Identity Services (GIS) bat popup xac thuc
   v
[ Google OAuth2 Server ]
   |
   | 3. Tra ve Google ID Token (credential JWT)
   v
[ Frontend: Next.js ]
   |
   | 4. POST /api/v1/auth/google { credential: "..." }
   v
[ Backend: AuthController -> AuthService -> GoogleAuthService ]
   |
   | 5. Goi GET https://oauth2.googleapis.com/tokeninfo?id_token=...
   v
[ Google Tokeninfo API ]
   |
   | 6. Tra ve payload (email, email_verified, sub, name, picture)
   v
[ Backend: AuthService ]
   |
   | 7. Kiem tra tai khoan trong PostgreSQL:
   |    - Tim theo providerId va authProvider = "GOOGLE" hoac theo email
   |    - Neu chua co: Tao User moi (username duy nhat, role ROLE_MEMBER, random password)
   |    - Neu da co: Cap nhat providerId va avatar neu can
   | 8. Kiem tra trang thai (BANNED thi chan)
   | 9. Sinh Access Token (JWT) & Refresh Token (Redis)
   v
[ Frontend: Next.js ]
   |
   | 10. Luu Access Token + User vao Zustand Store & LocalStorage
   | 11. Nhan Refresh Token qua HttpOnly Cookie
```

---

## 3. Thay doi Schema & Database

- **Bang `users`:**
  - Them cot `auth_provider` (`VARCHAR(20)`, mac dinh `'LOCAL'`).
  - Them cot `provider_id` (`VARCHAR(100)`), luu `sub` duy nhat tu Google.

---

## 4. Danh sach API Endpoints

### 4.1. Dang nhap / Dang ky Google

- **URL:** `POST /api/v1/auth/google`
- **Quyen truy cap:** Public (`permitAll`)
- **Request Body:**
```json
{
  "credential": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```
- **Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "refreshToken": "7c9e6679-7425-40de-944b-...",
  "user": {
    "id": 12,
    "username": "nguyen_van_a",
    "email": "a.nguyen@gmail.com",
    "avatarUrl": "https://lh3.googleusercontent.com/...",
    "authProvider": "GOOGLE",
    "userStatus": "ACTIVE",
    "roles": ["ROLE_MEMBER"],
    "createdAt": "2026-11-13T12:30:00"
  }
}
```
- **Set-Cookie Header:**
  - `refreshToken=<token>; Path=/api/v1/auth; HttpOnly; SameSite=Lax; Max-Age=604800`

---

## 5. Danh sach file thay doi & bo sung

### Backend (`struyen_be`):
1. `src/main/java/com/struyen/struyen/entities/User.java`: Them cac truong `authProvider` va `providerId`.
2. `src/main/java/com/struyen/struyen/repositories/UserRepository.java`: Them `findWithRolesByAuthProviderAndProviderId`.
3. `src/main/java/com/struyen/struyen/dtos/UserDto.java`: Them `authProvider` vao DTO.
4. `src/main/java/com/struyen/struyen/dtos/GoogleLoginRequest.java`: DTO request chua Google credential.
5. `src/main/java/com/struyen/struyen/dtos/GoogleTokenInfoDto.java`: DTO nhan thong tin tu Google Tokeninfo API.
6. `src/main/java/com/struyen/struyen/services/GoogleAuthService.java`: Service xac thuc ID Token tu Google.
7. `src/main/java/com/struyen/struyen/services/AuthService.java`: Logic `loginWithGoogle` va tao username duy nhat.
8. `src/main/java/com/struyen/struyen/controllers/AuthController.java`: Endpoint `POST /api/v1/auth/google`.
9. `src/main/java/com/struyen/struyen/config/SecurityConfig.java`: Cho phep truy cap cong khai `POST /api/v1/auth/google`.
10. `src/main/resources/application.properties`: Them cau hinh `google.client-id`.
11. `src/test/java/com/struyen/struyen/services/GoogleAuthServiceTest.java`: Unit test cho GoogleAuthService.
12. `src/test/java/com/struyen/struyen/services/AuthServiceTest.java`: Unit test cho luong dang nhap Google.

### Frontend (`komika-fe`):
1. `src/lib/api/backend-types.ts`: Them `authProvider` vao `UserDto` va interface `GoogleLoginRequest`.
2. `src/features/auth/api/auth.api.ts`: Them ham `loginWithGoogleApi`.
3. `src/features/auth/hooks/useAuthQueries.ts`: Them hook `useGoogleLoginMutation`.
4. `src/features/auth/components/AuthDialog.tsx`: Tich hop Google Identity Services SDK va nut xac thuc Google.

---

## 6. Kiem thu & Xac minh (Verification)

1. **Unit Tests Backend:**
   - `GoogleAuthServiceTest`: 4/4 test cases pass (Verify hop le, token trong, email chua xac minh, Google API loi).
   - `AuthServiceTest`: 10/10 test cases pass (Tao user moi, lien ket user cu, chan user bi banned).
   - Toan bo test suite: 47/47 tests pass (`BUILD SUCCESS`).
2. **Frontend Type Checking:**
   - `npx tsc --project tsconfig.json --noEmit`: 0 loi, pass 100%.
3. **Cau hinh moi truong:**
   - Frontend: `NEXT_PUBLIC_GOOGLE_CLIENT_ID` trong `.env.local`.
   - Backend: `GOOGLE_CLIENT_ID` trong `application.properties` (hoac bien moi truong).

