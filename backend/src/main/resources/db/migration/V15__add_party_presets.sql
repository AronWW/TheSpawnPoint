CREATE TABLE party_presets (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(50)  NOT NULL,
    slot_index  INT          NOT NULL,
    game_id     BIGINT       NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    max_members INT          NOT NULL DEFAULT 4,
    platform    VARCHAR(20)[] DEFAULT '{}',
    languages   VARCHAR(50)[] DEFAULT '{}',
    skill_level VARCHAR(20),
    play_style  VARCHAR(20),
    tags        VARCHAR(30)[] DEFAULT '{}',
    region      VARCHAR(30),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_slot UNIQUE (user_id, slot_index),
    CONSTRAINT chk_slot_index CHECK (slot_index >= 0 AND slot_index < 10)
);

