CREATE TABLE IF NOT EXISTS image_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_image_user (image_id, user_id),
    INDEX idx_image_id (image_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
