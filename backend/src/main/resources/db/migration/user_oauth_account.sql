-- ==========================================
-- Microsoft OAuth2 第三方账号绑定表
-- ==========================================
-- 请在数据库中执行此 SQL 来创建 user_oauth_account 表
-- 此表用于存储第三方 OAuth 账号与本地用户的绑定关系
-- ==========================================

CREATE TABLE IF NOT EXISTS user_oauth_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '本地用户ID',
    provider VARCHAR(32) NOT NULL COMMENT '第三方平台，例如 microsoft/github/google',
    provider_user_id VARCHAR(128) NOT NULL COMMENT '第三方平台用户唯一ID',
    provider_email VARCHAR(255) DEFAULT NULL COMMENT '第三方邮箱',
    provider_username VARCHAR(255) DEFAULT NULL COMMENT '第三方用户名/显示名',
    avatar_url VARCHAR(1024) DEFAULT NULL COMMENT '第三方头像URL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_provider_user (provider, provider_user_id),
    KEY idx_user_id (user_id)
) COMMENT='第三方账号绑定表';
