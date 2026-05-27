-- Picture Management System Database Schema

CREATE DATABASE IF NOT EXISTS picture_management
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE picture_management;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(50),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    avatar LONGTEXT,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    github_username VARCHAR(100),
    avatar_key VARCHAR(500),
    background_key VARCHAR(500),
    bio VARCHAR(200),
    background LONGTEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_name VARCHAR(20) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Images table (image_path stores Base64 Data URL)
CREATE TABLE IF NOT EXISTS images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT,
    image_name VARCHAR(255) NOT NULL,
    image_path LONGTEXT NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    image_type VARCHAR(20) NOT NULL,
    description TEXT,
    tags VARCHAR(500),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    visible_usernames VARCHAR(500),
    upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id),
    INDEX idx_visibility (visibility),
    INDEX idx_upload_time (upload_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Comments table (image_path stores Base64 Data URL)
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    image_path LONGTEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_image_id (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Image likes table
CREATE TABLE IF NOT EXISTS image_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_image_user (image_id, user_id),
    INDEX idx_image_id (image_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    comment_id BIGINT,
    type VARCHAR(20) NOT NULL,
    content_preview VARCHAR(255),
    read_flag TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_recipient_read_time (recipient_user_id, read_flag, create_time),
    INDEX idx_image_id (image_id),
    INDEX idx_actor_user_id (actor_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- RBAC tables
-- Permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    group_name VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Role-permission mapping
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User-role mapping
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed roles
INSERT IGNORE INTO roles (id, code, name) VALUES (1, 'admin', '管理员');
INSERT IGNORE INTO roles (id, code, name) VALUES (2, 'moderator', '版主');
INSERT IGNORE INTO roles (id, code, name) VALUES (3, 'user', '普通用户');

-- Seed permissions
INSERT IGNORE INTO permissions (id, code, name, group_name) VALUES
(1, 'image:upload', '上传图片', 'image'),
(2, 'image:edit', '编辑自己的图片', 'image'),
(3, 'image:delete', '删除自己的图片', 'image'),
(4, 'image:edit:any', '编辑任意图片', 'image'),
(5, 'image:delete:any', '删除任意图片', 'image'),
(6, 'category:manage', '管理自己的分类', 'category'),
(7, 'category:manage:any', '管理任意分类', 'category'),
(8, 'comment:add', '添加评论', 'comment'),
(9, 'comment:delete', '删除自己的评论', 'comment'),
(10, 'comment:delete:any', '删除任意评论', 'comment'),
(11, 'user:manage', '管理用户', 'admin');

-- Seed role_permissions (admin gets all)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

-- moderator permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE code IN ('comment:delete:any', 'image:edit:any');

-- user permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE code IN ('image:upload', 'image:edit', 'image:delete', 'category:manage', 'comment:add', 'comment:delete');
