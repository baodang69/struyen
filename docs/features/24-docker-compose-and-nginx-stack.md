# Feature F-24: Docker Compose & Nginx Production Stack

Dong goi toan dien he thong Struyen (Backend Spring Boot, Frontend Next.js Standalone, Nginx Reverse Proxy Cache, PostgreSQL 16 va Redis 7) bang Docker va Docker Compose san sang trien khai moi truong san xuat / VMware / Cloudflare Tunnel.

---

## 1. Muc tieu & Yeu cau he thong

1. **Dong goi ung dung Backend (Spring Boot 3.5 / Java 21):**
   - Su dung Multi-stage build tren `maven:3.9.9-eclipse-temurin-21-alpine` de cache dependency rieng biet.
   - Chay ung dung tren `eclipse-temurin:21-jre-alpine` sieu nhe, bao mat (non-root user).
   - Tich hop container healthcheck thong qua `/actuator/health`.
2. **Dong goi ung dung Frontend (Next.js 16 / React 18):**
   - Kich hoat che do `output: "standalone"` de chi dong goi bundle toi gian va dependency can thiet, giam dung luong image tu ~1.2GB xuong ~120MB.
   - Non-root user `nextjs:nodejs` de dam bao an toan container.
3. **Nginx Reverse Proxy & Proxy Cache (Toi uu anh truyen):**
   - Dieu huong luong truy cap duy nhat qua cong 80 (hoac 443 khi qua Cloudflare Tunnel):
     - `/api/` -> Chuyen tiep den `backend:8080`.
     - `/` va `/_next/` -> Chuyen tiep den `frontend:3000`.
   - Caching o dia cuc bo (`proxy_cache`) cho `/api/v1/proxy/image`: Cache anh truyen Otruyen trong 30 ngay, gioi han dung luong 5GB, co che `proxy_cache_lock` ngan chan cache stampede.
4. **Moi truong trien khai (Docker Compose):**
   - `docker-compose.yml`: Trien khai build local / dev full-stack.
   - `docker-compose.prod.yml`: Trien khai san xuat keo image tu GitHub Container Registry (GHCR) kem container Watchtower tu dong cap nhat khong can SSH.
   - `.env.docker.example`: File mau bien moi truong he thong.

---

## 2. Kien truc He thong Container

```
[ Internet / Cloudflare Tunnel / Browser ]
                       |
                       v
         [ struyen-nginx (Port 80/443) ]
         /              |              \
        /               |               \
       /                |                \
  (Static / SSR)      (/api/)      (/api/v1/proxy/image)
     v                  v                 v
[ struyen-frontend ]  [ struyen-backend ]  [ Nginx Disk Cache ]
   (Next.js:3000)        (Spring:8080)     (5GB, 30d TTL)
                                |
                     +----------+----------+
                     |                     |
                     v                     v
            [ struyen-postgres ]   [ struyen-redis ]
                 (Port 5432)          (Port 6379)
```

---

## 3. Danh sach file tao moi va cap nhat

### 3.1. Backend (`struyen_be`)
- `Dockerfile`: Multi-stage build Maven + Eclipse Temurin 21 JRE Alpine.
- `.dockerignore`: Loai bo target, git, env khoi build context.
- `.env.docker.example`: Mau bien moi truong day du cho Database, Redis, JWT, Google OAuth.
- `docker-compose.yml`: Cau hinh toan bo dich vu (Postgres, Redis, Backend, Frontend, Nginx).
- `docker-compose.prod.yml`: Cau hinh production su dung GHCR va Watchtower.
- `docker/nginx/nginx.conf`: Cau hinh Nginx reverse proxy va caching anh.
- `.github/workflows/build-and-push.yml`: CI/CD tu dong build va day backend image len GHCR.
- `src/main/java/com/struyen/struyen/config/SecurityConfig.java`: Cho phep `/actuator/health` va `/actuator/info` khong can xac thuc.
- `src/main/resources/application.properties`: Kich hoat Actuator health probes.

### 3.2. Frontend (`komika-fe`)
- `Dockerfile`: Multi-stage build Node 20 Alpine standalone mode.
- `.dockerignore`: Loai bo node_modules, .next, git, env khoi build context.
- `next.config.mjs`: Kich hoat `output: "standalone"`.
- `src/lib/api/http-client.ts`: Ho tro `NEXT_PUBLIC_API_BASE_URL` rong de request tu dong di qua Nginx reverse proxy (same-origin).
- `.github/workflows/build-and-push.yml`: CI/CD tu dong build va day frontend image len GHCR.

---

## 4. Huong dan su dung

### 4.1. Chay toan bo he thong tren may noi bo (Build tu source)
```bash
# 1. Chuyen vao thu muc backend
cd d:/Code/struyen_be

# 2. Tao file bien moi truong tu file mau
cp .env.docker.example .env

# 3. Khoi dong toan bo he thong
docker-compose up -d --build

# 4. Kiem tra trang thai cac container
docker-compose ps
```

### 4.2. Trien khai tren may ao VMware (Su dung GHCR)
```bash
# 1. Chuan bi thu muc tren server
mkdir -p /opt/struyen && cd /opt/struyen

# 2. Copy docker-compose.prod.yml, .env va thu muc docker/nginx/
# 3. Chay he thong
docker compose -f docker-compose.prod.yml up -d
```

---

## 5. Kiem tra & Xac minh (Verification)

1. **Kiem tra cu phap Compose:**
   - `docker-compose -f docker-compose.yml config`: Hop le (Exit code 0).
   - `docker-compose -f docker-compose.prod.yml config`: Hop le (Exit code 0).
2. **Kiem tra Test Suite Backend:**
   - `./mvnw test`: 48/48 tests passed (0 failures, 0 errors).

