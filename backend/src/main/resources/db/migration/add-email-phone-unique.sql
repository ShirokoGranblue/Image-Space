-- Normalize existing empty strings to NULL (MySQL UNIQUE allows multiple NULLs)
UPDATE users SET email = NULL WHERE email = '';
UPDATE users SET phone = NULL WHERE phone = '';

ALTER TABLE users ADD UNIQUE (email);
ALTER TABLE users ADD UNIQUE (phone);
