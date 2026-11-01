-- Tracking contract v1.0. Existing rows remain queryable.
ALTER TABLE user_event
    ADD COLUMN event_id VARCHAR(64) NULL,
    ADD COLUMN anonymous_id VARCHAR(64) NULL,
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'server',
    ADD COLUMN consent_state VARCHAR(30) NOT NULL DEFAULT 'server',
    ADD COLUMN schema_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    ADD COLUMN context_json TEXT NULL,
    ADD COLUMN occurred_at DATETIME NULL;

UPDATE user_event SET event_id = CONCAT('legacy-', id), occurred_at = created_at_v2
WHERE event_id IS NULL;

ALTER TABLE user_event
    MODIFY event_id VARCHAR(64) NOT NULL,
    ADD UNIQUE INDEX ux_user_event_event_id (event_id),
    ADD INDEX idx_user_event_source (source, event_type, created_at_v2);
