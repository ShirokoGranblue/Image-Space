ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified TINYINT NOT NULL DEFAULT 0 AFTER email;

UPDATE users
SET email_verified = 0
WHERE email IS NULL OR email = '';
