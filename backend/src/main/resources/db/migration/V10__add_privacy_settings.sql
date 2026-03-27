CREATE TABLE privacy_settings (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    friends_visibility VARCHAR(10) NOT NULL DEFAULT 'ALL',
    status_visibility VARCHAR(10) NOT NULL DEFAULT 'ALL',
    favorite_games_visibility VARCHAR(10) NOT NULL DEFAULT 'ALL',
    stats_visibility VARCHAR(10) NOT NULL DEFAULT 'ALL',
    socials_visibility VARCHAR(10) NOT NULL DEFAULT 'ALL',
    comments_policy VARCHAR(10) NOT NULL DEFAULT 'ALL'
);

INSERT INTO privacy_settings (user_id, friends_visibility, status_visibility, favorite_games_visibility, stats_visibility, socials_visibility, comments_policy)
SELECT id, 'ALL', 'ALL', 'ALL', 'ALL', 'ALL', 'ALL'
FROM users
ON CONFLICT DO NOTHING;

