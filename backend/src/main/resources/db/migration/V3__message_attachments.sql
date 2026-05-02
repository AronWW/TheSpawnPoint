CREATE TABLE IF NOT EXISTS message_attachments
(
    id                BIGSERIAL    PRIMARY KEY,
    message_id        BIGINT       NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    position          INT          NOT NULL,
    media_type        VARCHAR(30)  NOT NULL,
    url               VARCHAR(1000) NOT NULL,
    public_id         VARCHAR(500) NOT NULL,
    resource_type     VARCHAR(20)  NOT NULL,
    original_filename VARCHAR(255),
    content_type      VARCHAR(100),
    size_bytes        BIGINT       NOT NULL,
    width             INT,
    height            INT,
    duration_seconds  NUMERIC(10, 2),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_message_attachment_position UNIQUE (message_id, position),
    CONSTRAINT chk_message_attachment_position CHECK (position >= 0 AND position < 5),
    CONSTRAINT chk_message_attachment_size CHECK (size_bytes > 0),
    CONSTRAINT chk_message_attachment_media_type CHECK (
        media_type IN ('IMAGE', 'GIF', 'VIDEO', 'AUDIO', 'TEXT_FILE', 'PDF', 'FILE')
    ),
    CONSTRAINT chk_message_attachment_resource_type CHECK (
        resource_type IN ('image', 'video', 'raw')
    )
);

CREATE INDEX IF NOT EXISTS idx_message_attachments_message
    ON message_attachments (message_id);
