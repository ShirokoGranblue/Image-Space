package com.picmgmt.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS audit_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT,
                username VARCHAR(100),
                action VARCHAR(100) NOT NULL,
                module VARCHAR(100) NOT NULL,
                target_type VARCHAR(100),
                target_id VARCHAR(100),
                method VARCHAR(10),
                path VARCHAR(500),
                ip VARCHAR(64),
                user_agent VARCHAR(500),
                request_params TEXT,
                result VARCHAR(20) NOT NULL,
                error_message VARCHAR(1000),
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_user_id (user_id),
                INDEX idx_action (action),
                INDEX idx_module (module),
                INDEX idx_create_time (create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
    }
}
