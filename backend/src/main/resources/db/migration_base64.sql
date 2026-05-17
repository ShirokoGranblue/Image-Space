-- ============================================================
-- 迁移脚本：将图片存储从文件路径改为 Base64 Data URL
-- 执行前请先备份数据库！
-- ============================================================

USE picture_management;

-- 1. images 表：image_path 从 VARCHAR(500) 改为 LONGTEXT
ALTER TABLE images MODIFY COLUMN image_path LONGTEXT NOT NULL;

-- 2. users 表：增加 avatar、background 等字段（如已存在则忽略）
-- 注意：MySQL 不支持 ADD COLUMN IF NOT EXISTS，请忽略已存在的错误
ALTER TABLE users ADD COLUMN display_name VARCHAR(50) DEFAULT NULL;
ALTER TABLE users ADD COLUMN avatar LONGTEXT DEFAULT NULL;
ALTER TABLE users ADD COLUMN email VARCHAR(100) DEFAULT NULL;
ALTER TABLE users ADD COLUMN phone VARCHAR(20) DEFAULT NULL;
ALTER TABLE users ADD COLUMN bio VARCHAR(200) DEFAULT NULL;
ALTER TABLE users ADD COLUMN background LONGTEXT DEFAULT NULL;

-- 3. comments 表：增加 image_path 字段
ALTER TABLE comments ADD COLUMN image_path LONGTEXT DEFAULT NULL;
