ALTER TABLE chat_participants
    ADD COLUMN IF NOT EXISTS last_read_message_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_chat_participants_last_read_message'
    ) THEN
        ALTER TABLE chat_participants
            ADD CONSTRAINT fk_chat_participants_last_read_message
                FOREIGN KEY (last_read_message_id)
                REFERENCES messages (id)
                ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_chat_participants_last_read_message
    ON chat_participants (last_read_message_id);
