-- ==========================================
-- STRUYEN DATABASE SCHEMA (POSTGRESQL)
-- ==========================================

-- ==========================================
-- 1. NHÓM DỮ LIỆU CỐT LÕI (CORE DATA)
-- ==========================================

-- Bảng Category (Thể loại)
CREATE TABLE IF NOT EXISTS category (
    id VARCHAR(255) PRIMARY KEY, -- ID từ API
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_category_slug ON category (slug);

-- Bảng Story (Truyện)
CREATE TABLE IF NOT EXISTS story (
    id VARCHAR(255) PRIMARY KEY, -- ID từ API
    slug VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(255), -- ongoing, completed...
    thumb_url VARCHAR(255),
    cdn_domain VARCHAR(255),
    description TEXT,
    author VARCHAR(255),
    view_count BIGINT DEFAULT 0,
    
    -- TRƯỜNG CHO ADMIN & SYNC
    is_active BOOLEAN DEFAULT TRUE,      -- Admin dùng để ẩn/hiện truyện
    is_featured BOOLEAN DEFAULT FALSE,   -- Đưa lên mục đề cử trang chủ
    origin_updated_at TIMESTAMP WITHOUT TIME ZONE, -- Thời gian update phía API để so sánh sync
    
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMP WITHOUT TIME ZONE,
    last_chapter_sync_at TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_story_slug ON story (slug);
CREATE INDEX IF NOT EXISTS idx_story_updated_at ON story (updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_story_active ON story (is_active) WHERE is_active = TRUE;

-- Bảng trung gian Story <-> Category
CREATE TABLE IF NOT EXISTS story_category (
    id BIGSERIAL PRIMARY KEY,
    story_id VARCHAR(255) REFERENCES story(id) ON DELETE CASCADE,
    category_id VARCHAR(255) REFERENCES category(id) ON DELETE CASCADE,
    CONSTRAINT uq_story_id_category_id UNIQUE (story_id, category_id)
);
CREATE INDEX IF NOT EXISTS idx_story_category_category_id ON story_category (category_id);


-- Bảng Chapter (Danh sách chương)
CREATE TABLE IF NOT EXISTS chapter (
    id VARCHAR(255) PRIMARY KEY, -- ID từ API
    story_id VARCHAR(255) REFERENCES story(id) ON DELETE CASCADE,
    chapter_name VARCHAR(255) NOT NULL,
    chapter_title VARCHAR(255),
    chapter_api_data VARCHAR(255) UNIQUE NOT NULL, -- URL gọi mảng ảnh
    
    -- Lưu mảng ảnh trực tiếp bằng JSONB để đạt hiệu năng cao nhất
    images_json JSONB, -- Cấu trúc: [{"file": "abc.jpg", "page": 1}, ...]
    
    is_content_synced BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (story_id, chapter_name)
);
CREATE INDEX IF NOT EXISTS idx_chapter_story ON chapter (story_id);

-- ==========================================
-- 2. NHÓM NGƯỜI DÙNG (USER & AUTH)
-- ==========================================

-- Bảng Users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    avatar_url VARCHAR(255),
    user_status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL, -- ACTIVE, BANNED
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Role
CREATE TABLE IF NOT EXISTS role (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL, -- ADMIN, MEMBER
    description VARCHAR(255)
);

-- Bảng trung gian User <-> Role
CREATE TABLE IF NOT EXISTS user_role (
    id SERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER REFERENCES role(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

-- ==========================================
-- 3. NHÓM TƯƠNG TÁC (INTERACTION)
-- ==========================================

-- Bảng Subscribe (Theo dõi truyện)
CREATE TABLE IF NOT EXISTS story_subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    story_id VARCHAR(255) REFERENCES story(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_subscription_story_id_user_id UNIQUE (story_id, user_id)
);

-- Bảng Comment (Bình luận & Reply)
CREATE TABLE IF NOT EXISTS comment (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    story_id VARCHAR(255) REFERENCES story(id) ON DELETE CASCADE,
    chapter_id VARCHAR(255) REFERENCES chapter(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES comment(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    like_count INTEGER DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_comment_story ON comment (story_id);
CREATE INDEX IF NOT EXISTS idx_comment_chapter ON comment (chapter_id);

-- Bảng Reading History (Lịch sử đọc)
CREATE TABLE IF NOT EXISTS reading_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    story_id VARCHAR(255) REFERENCES story(id) ON DELETE CASCADE,
    last_chapter_id VARCHAR(255) REFERENCES chapter(id) ON DELETE SET NULL,
    reading_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reading_history UNIQUE (user_id, story_id)
);

-- ==========================================
-- 4. NHÓM HỆ THỐNG & THÔNG BÁO (SYSTEM & NOTIFICATION)
-- ==========================================

-- Thông báo chung từ Admin (Notice)
CREATE TABLE IF NOT EXISTS notice (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Thông báo riêng cho User (Khi truyện theo dõi có chương mới)
CREATE TABLE IF NOT EXISTS user_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255),
    message TEXT,
    link_url VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_notif_user ON user_notification (user_id, is_read);

