-- V13: Add avatar_public_id to chats for Cloudinary cleanup

ALTER TABLE chats ADD COLUMN avatar_public_id VARCHAR(255);

