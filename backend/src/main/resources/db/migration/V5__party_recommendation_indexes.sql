CREATE INDEX IF NOT EXISTS idx_party_requests_status_created_at
    ON party_requests (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_party_requests_game_id
    ON party_requests (game_id);

CREATE INDEX IF NOT EXISTS idx_party_requests_creator_id
    ON party_requests (creator_id);

CREATE INDEX IF NOT EXISTS idx_party_requests_region
    ON party_requests (region);

CREATE INDEX IF NOT EXISTS idx_party_requests_skill_level
    ON party_requests (skill_level);

CREATE INDEX IF NOT EXISTS idx_party_requests_play_style
    ON party_requests (play_style);

CREATE INDEX IF NOT EXISTS idx_party_requests_platform_gin
    ON party_requests USING GIN (platform);

CREATE INDEX IF NOT EXISTS idx_party_requests_languages_gin
    ON party_requests USING GIN (languages);

CREATE INDEX IF NOT EXISTS idx_party_members_party_request_id
    ON party_members (party_request_id);

CREATE INDEX IF NOT EXISTS idx_party_members_user_id
    ON party_members (user_id);

CREATE INDEX IF NOT EXISTS idx_user_games_user_game
    ON user_games (user_id, game_id);

CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked_blocker
    ON user_blocks (blocked_id, blocker_id);
