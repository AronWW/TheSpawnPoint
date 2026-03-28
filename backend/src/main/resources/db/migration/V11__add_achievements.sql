ALTER TABLE privacy_settings
    ADD COLUMN IF NOT EXISTS achievements_visibility VARCHAR(10) NOT NULL DEFAULT 'ALL';

CREATE TABLE IF NOT EXISTS user_achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_code VARCHAR(100) NOT NULL,
    source VARCHAR(20) NOT NULL,
    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_achievement_code UNIQUE (user_id, achievement_code)
);

CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_code ON user_achievements(achievement_code);
