-- migration_user_event.sql
-- Thêm bảng user_event để lưu sự kiện tracking người dùng
-- Chạy thủ công qua HeidiSQL hoặc mysql CLI, KHÔNG auto run qua JPA

CREATE TABLE IF NOT EXISTS user_event (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    username   VARCHAR(50) NULL,
    event_type VARCHAR(40) NOT NULL,
    product_id BIGINT NULL,
    page       VARCHAR(200),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_user (username, created_at)
);