ALTER TABLE user_achievements
    ADD COLUMN IF NOT EXISTS featured_position INT;

ALTER TABLE user_achievements
    DROP CONSTRAINT IF EXISTS chk_user_achievement_featured_position;

ALTER TABLE user_achievements
    ADD CONSTRAINT chk_user_achievement_featured_position CHECK (
        featured_position IS NULL OR (featured_position >= 0 AND featured_position < 4)
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_achievement_featured_position
    ON user_achievements (user_id, featured_position)
    WHERE featured_position IS NOT NULL;
