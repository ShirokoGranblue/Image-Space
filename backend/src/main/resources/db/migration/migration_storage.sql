-- Add new storage_key columns for MinIO object storage
ALTER TABLE images ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255) AFTER image_path;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_key VARCHAR(255) AFTER avatar;
ALTER TABLE users ADD COLUMN IF NOT EXISTS background_key VARCHAR(255) AFTER background;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS image_key VARCHAR(255) AFTER image_path;
