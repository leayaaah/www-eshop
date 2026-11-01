-- Run after migration_user_event.sql. This migration preserves existing events.
ALTER TABLE user_event
    ADD COLUMN value_ms INT NULL,
    ADD COLUMN created_at_v2 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE user_event SET created_at_v2 = created_at WHERE created_at IS NOT NULL;

ALTER TABLE user_event
    ADD INDEX idx_ue_session_time (session_id, created_at_v2),
    ADD INDEX idx_ue_user_time (username, created_at_v2),
    ADD INDEX idx_ue_product_type (product_id, event_type);

-- New code maps created_at_v2 to the JPA createdAt field.
