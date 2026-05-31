-- Add UUID columns for external-facing URLs, keeping numeric IDs for internal FK usage
-- Step 1: Add columns without UNIQUE constraint first
ALTER TABLE users ADD COLUMN uuid VARCHAR(36) NOT NULL DEFAULT '';
ALTER TABLE images ADD COLUMN uuid VARCHAR(36) NOT NULL DEFAULT '';

-- Step 2: Backfill existing rows with unique UUIDs
UPDATE users SET uuid = UUID() WHERE uuid = '';
UPDATE images SET uuid = UUID() WHERE uuid = '';

-- Step 3: Add UNIQUE constraint after backfill
ALTER TABLE users ADD UNIQUE (uuid);
ALTER TABLE images ADD UNIQUE (uuid);
