ALTER TABLE images
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN visible_usernames VARCHAR(500),
    ADD INDEX idx_visibility (visibility);
