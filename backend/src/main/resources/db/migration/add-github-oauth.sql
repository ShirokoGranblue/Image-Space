-- Add github_username, avatar_key, background_key columns
ALTER TABLE users ADD COLUMN github_username VARCHAR(100) AFTER phone;
ALTER TABLE users ADD COLUMN avatar_key VARCHAR(500) AFTER github_username;
ALTER TABLE users ADD COLUMN background_key VARCHAR(500) AFTER avatar_key;
