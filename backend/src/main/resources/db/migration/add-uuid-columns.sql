-- Add UUID columns for external-facing URLs, keeping numeric IDs for internal FK usage

ALTER TABLE users ADD COLUMN uuid VARCHAR(36) NOT NULL DEFAULT '' UNIQUE;
ALTER TABLE images ADD COLUMN uuid VARCHAR(36) NOT NULL DEFAULT '' UNIQUE;

-- Backfill existing rows
UPDATE users SET uuid = UUID() WHERE uuid = '';
UPDATE images SET uuid = UUID() WHERE uuid = '';
