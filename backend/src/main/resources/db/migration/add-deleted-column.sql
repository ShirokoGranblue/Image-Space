-- Add deleted column for soft-delete
ALTER TABLE users ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 AFTER create_time;
