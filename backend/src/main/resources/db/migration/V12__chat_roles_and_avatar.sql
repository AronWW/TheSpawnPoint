-- V12: Group chat roles (OWNER / ADMIN / MEMBER) and group avatar

ALTER TABLE chat_participants ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'MEMBER';

ALTER TABLE chats ADD COLUMN avatar_url VARCHAR(500);

