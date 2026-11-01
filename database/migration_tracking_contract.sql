-- Tracking contract v1.0. Existing rows remain queryable.
ALTER TABLE user_event
    ADD COLUMN IF NOT EXISTS event_id VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS anonymous_id VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'server',
    ADD COLUMN IF NOT EXISTS consent_state VARCHAR(30) NOT NULL DEFAULT 'server',
    ADD COLUMN IF NOT EXISTS schema_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    ADD COLUMN IF NOT EXISTS context_json TEXT NULL,
    ADD COLUMN IF NOT EXISTS occurred_at DATETIME NULL;

UPDATE user_event SET event_id = CONCAT('legacy-', id), occurred_at = created_at_v2
WHERE event_id IS NULL;

ALTER TABLE user_event
    MODIFY event_id VARCHAR(64) NOT NULL,
    ADD UNIQUE INDEX IF NOT EXISTS ux_user_event_event_id (event_id),
    ADD INDEX IF NOT EXISTS idx_user_event_source (source, event_type, created_at_v2);
