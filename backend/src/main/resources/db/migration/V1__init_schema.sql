
DO $$ BEGIN
    CREATE TYPE invite_status AS ENUM ('PENDING', 'ACCEPTED', 'DECLINED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE suggestion_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE report_reason AS ENUM (
        'TOXIC_BEHAVIOR', 'CHEATING', 'SPAM', 'HARASSMENT',
        'INAPPROPRIATE_CONTENT', 'OTHER'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE report_status AS ENUM ('OPEN', 'REVIEWED', 'DISMISSED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE ticket_status AS ENUM ('OPEN', 'IN_PROGRESS', 'CLOSED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE unban_request_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE notification_type AS ENUM (
        'FRIEND_REQUEST', 'PARTY_INVITE', 'MESSAGE', 'SYSTEM',
        'GAME_SUGGESTION_APPROVED', 'GAME_SUGGESTION_REJECTED',
        'REPORT_REVIEWED', 'SUPPORT_REPLY', 'UNBAN_REQUEST_REVIEWED'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users
(
    id             BIGSERIAL    PRIMARY KEY,
    display_name   VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status         VARCHAR(10)  NOT NULL DEFAULT 'OFFLINE',
    last_seen      TIMESTAMPTZ,
    banned         BOOLEAN      NOT NULL DEFAULT FALSE,
    ban_reason     TEXT,
    banned_at      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- profiles
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS profiles
(
    user_id          BIGINT       NOT NULL PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    full_name        VARCHAR(100) NOT NULL,
    avatar_url       VARCHAR(500),
    avatar_public_id VARCHAR(255),
    bio              TEXT,
    birth_date       DATE,
    platforms        VARCHAR(20)[],
    skill_level      VARCHAR(20),
    play_style       VARCHAR(20),
    languages        VARCHAR(50)[],
    country          VARCHAR(100),
    region           VARCHAR(30),
    discord          VARCHAR(100),
    steam            VARCHAR(200),
    twitch           VARCHAR(200),
    xbox             VARCHAR(200),
    playstation      VARCHAR(200),
    nintendo         VARCHAR(200),
    banner_url       VARCHAR(20),
    CONSTRAINT chk_region CHECK (
        region IS NULL OR region IN (
            'EUROPE', 'ASIA', 'NORTH_AMERICA', 'SOUTH_AMERICA',
            'OCEANIA', 'AFRICA', 'MIDDLE_EAST'
        )
    )
);

-- ------------------------------------------------------------
-- email_verification_tokens
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_verification_tokens
(
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code         VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    last_sent_at TIMESTAMPTZ  NOT NULL
);

-- ------------------------------------------------------------
-- password_reset_tokens
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_tokens
(
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ------------------------------------------------------------
-- games
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS games
(
    id             BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    genre          VARCHAR(50),
    release_year   SMALLINT,
    image_url      VARCHAR(500),
    max_party_size INTEGER      NOT NULL DEFAULT 5
);

-- ------------------------------------------------------------
-- game_suggestions
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS game_suggestions
(
    id             BIGSERIAL         PRIMARY KEY,
    suggested_by   BIGINT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name           VARCHAR(100)      NOT NULL,
    genre          VARCHAR(50),
    release_year   SMALLINT,
    image_url      VARCHAR(500),
    max_party_size INTEGER           NOT NULL DEFAULT 5,
    status         suggestion_status NOT NULL DEFAULT 'PENDING',
    admin_comment  TEXT,
    created_at     TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    reviewed_at    TIMESTAMPTZ
);

-- ------------------------------------------------------------
-- user_games
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_games
(
    id       BIGSERIAL   PRIMARY KEY,
    user_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id  BIGINT      NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, game_id)
);

-- ------------------------------------------------------------
-- friendships
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS friendships
(
    id            BIGSERIAL   PRIMARY KEY,
    user_id1      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_id2      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    friends_since TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id1, user_id2)
);

-- ------------------------------------------------------------
-- chats
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chats
(
    id               BIGSERIAL   PRIMARY KEY,
    title            VARCHAR(100),
    is_group         BOOLEAN     NOT NULL DEFAULT FALSE,
    party_linked     BOOLEAN     NOT NULL DEFAULT FALSE,
    avatar_url       VARCHAR(500),
    avatar_public_id VARCHAR(255),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- chat_participants
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_participants
(
    id         BIGSERIAL   PRIMARY KEY,
    chat_id    BIGINT      NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    role       VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    archived   BOOLEAN     NOT NULL DEFAULT FALSE,
    pinned     BOOLEAN     NOT NULL DEFAULT FALSE,
    pinned_at  TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    UNIQUE (chat_id, user_id)
);

-- ------------------------------------------------------------
-- messages
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS messages
(
    id          BIGSERIAL   PRIMARY KEY,
    chat_id     BIGINT      NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    sender_id   BIGINT      REFERENCES users (id),
    content     TEXT        NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read        BOOLEAN     NOT NULL DEFAULT FALSE,
    is_system   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    edited      BOOLEAN     NOT NULL DEFAULT FALSE,
    edited_at   TIMESTAMPTZ,
    reply_to_id BIGINT      REFERENCES messages (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_msg_chat   ON messages (chat_id);
CREATE INDEX IF NOT EXISTS idx_msg_sender ON messages (sender_id);
CREATE INDEX IF NOT EXISTS idx_msg_reply  ON messages (reply_to_id);

-- ------------------------------------------------------------
-- message_reactions
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS message_reactions
(
    id         BIGSERIAL   PRIMARY KEY,
    message_id BIGINT      NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    emoji      VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (message_id, user_id, emoji)
);

CREATE INDEX IF NOT EXISTS idx_reaction_msg ON message_reactions (message_id);

-- ------------------------------------------------------------
-- pinned_messages
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pinned_messages
(
    id         BIGSERIAL   PRIMARY KEY,
    chat_id    BIGINT      NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    message_id BIGINT      NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    pinned_by  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pinned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (chat_id, message_id)
);

-- ------------------------------------------------------------
-- party_requests
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS party_requests
(
    id              BIGSERIAL   PRIMARY KEY,
    creator_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id         BIGINT      NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    chat_id         BIGINT      UNIQUE REFERENCES chats (id) ON DELETE SET NULL,
    max_members     INTEGER     NOT NULL,
    is_open         BOOLEAN     NOT NULL DEFAULT TRUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    title           VARCHAR(150),
    description     TEXT,
    event_time      TIMESTAMPTZ,
    platform        VARCHAR(20)[],
    languages       VARCHAR(50)[],
    tags            VARCHAR(30)[],
    region          VARCHAR(30),
    skill_level     VARCHAR(20),
    play_style      VARCHAR(20),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    auto_completed  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_party_requests_status ON party_requests (status);

-- ------------------------------------------------------------
-- party_members
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS party_members
(
    id               BIGSERIAL   PRIMARY KEY,
    party_request_id BIGINT      NOT NULL REFERENCES party_requests (id) ON DELETE CASCADE,
    user_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (party_request_id, user_id)
);

-- ------------------------------------------------------------
-- invites
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invites
(
    id               BIGSERIAL     PRIMARY KEY,
    sender_id        BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    receiver_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type             VARCHAR(20)   NOT NULL DEFAULT 'FRIEND_REQUEST',
    status           invite_status NOT NULL DEFAULT 'PENDING',
    party_request_id BIGINT        REFERENCES party_requests (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    responded_at     TIMESTAMPTZ
);

-- ------------------------------------------------------------
-- notifications
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications
(
    id           BIGSERIAL         PRIMARY KEY,
    user_id      BIGINT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         notification_type NOT NULL,
    message      TEXT              NOT NULL,
    reference_id BIGINT,
    is_read      BOOLEAN           NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- user_reports
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_reports
(
    id               BIGSERIAL     PRIMARY KEY,
    reporter_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reported_user_id BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reason           report_reason NOT NULL,
    description      TEXT,
    status           report_status NOT NULL DEFAULT 'OPEN',
    admin_comment    TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    reviewed_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_reports_status ON user_reports (status);

-- ------------------------------------------------------------
-- support_tickets
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS support_tickets
(
    id         BIGSERIAL     PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    subject    VARCHAR(200)  NOT NULL,
    status     ticket_status NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    closed_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_tickets_status ON support_tickets (status);

-- ------------------------------------------------------------
-- support_messages
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS support_messages
(
    id        BIGSERIAL   PRIMARY KEY,
    ticket_id BIGINT      NOT NULL REFERENCES support_tickets (id) ON DELETE CASCADE,
    sender_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content   TEXT        NOT NULL,
    sent_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_support_msg_ticket ON support_messages (ticket_id);

-- ------------------------------------------------------------
-- unban_requests
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS unban_requests
(
    id            BIGSERIAL            PRIMARY KEY,
    user_id       BIGINT               NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reason        TEXT                 NOT NULL,
    status        unban_request_status NOT NULL DEFAULT 'PENDING',
    admin_comment TEXT,
    ban_reason    TEXT,
    created_at    TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    reviewed_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_unban_requests_status  ON unban_requests (status);
CREATE INDEX IF NOT EXISTS idx_unban_requests_user_id ON unban_requests (user_id);

-- ------------------------------------------------------------
-- profile_comments
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS profile_comments
(
    id              BIGSERIAL   PRIMARY KEY,
    profile_user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    author_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profile_comments_profile ON profile_comments (profile_user_id, created_at DESC);

-- ------------------------------------------------------------
-- privacy_settings
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS privacy_settings
(
    user_id                   BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    friends_visibility        VARCHAR(10) NOT NULL DEFAULT 'ALL',
    status_visibility         VARCHAR(10) NOT NULL DEFAULT 'ALL',
    favorite_games_visibility VARCHAR(10) NOT NULL DEFAULT 'ALL',
    stats_visibility          VARCHAR(10) NOT NULL DEFAULT 'ALL',
    socials_visibility        VARCHAR(10) NOT NULL DEFAULT 'ALL',
    comments_policy           VARCHAR(10) NOT NULL DEFAULT 'ALL',
    achievements_visibility   VARCHAR(10) NOT NULL DEFAULT 'ALL'
);

-- ------------------------------------------------------------
-- user_achievements
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_achievements
(
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    achievement_code VARCHAR(100) NOT NULL,
    source           VARCHAR(20)  NOT NULL,
    unlocked_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_achievement_code UNIQUE (user_id, achievement_code)
);

CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON user_achievements (user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_code    ON user_achievements (achievement_code);

-- ------------------------------------------------------------
-- user_blocks
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_blocks
(
    id         BIGSERIAL   PRIMARY KEY,
    blocker_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_blocks    UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_no_self_block CHECK (blocker_id <> blocked_id)
);

CREATE INDEX IF NOT EXISTS idx_user_blocks_blocker ON user_blocks (blocker_id);
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked ON user_blocks (blocked_id);

-- ------------------------------------------------------------
-- party_presets
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS party_presets
(
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(50)  NOT NULL,
    slot_index  INT          NOT NULL,
    game_id     BIGINT       NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    max_members INT          NOT NULL DEFAULT 4,
    platform    VARCHAR(20)[]  DEFAULT '{}',
    languages   VARCHAR(50)[]  DEFAULT '{}',
    skill_level VARCHAR(20),
    play_style  VARCHAR(20),
    tags        VARCHAR(30)[]  DEFAULT '{}',
    region      VARCHAR(30),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_slot   UNIQUE (user_id, slot_index),
    CONSTRAINT chk_slot_index CHECK (slot_index >= 0 AND slot_index < 10)
);

-- ------------------------------------------------------------
-- player_ratings
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS player_ratings
(
    id            BIGSERIAL   PRIMARY KEY,
    party_id      BIGINT      NOT NULL REFERENCES party_requests (id) ON DELETE CASCADE,
    rater_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rated_user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    score         INTEGER     NOT NULL CHECK (score BETWEEN 1 AND 5),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_rating_per_pair_per_party UNIQUE (party_id, rater_id, rated_user_id),
    CONSTRAINT chk_not_self_rate CHECK (rater_id <> rated_user_id)
);

CREATE INDEX IF NOT EXISTS idx_player_ratings_rated_user ON player_ratings (rated_user_id);
CREATE INDEX IF NOT EXISTS idx_player_ratings_party      ON player_ratings (party_id);
CREATE INDEX IF NOT EXISTS idx_player_ratings_rater      ON player_ratings (rater_id);
