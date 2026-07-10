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
                response_result TEXT,
                result VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
                risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
                cost_time BIGINT DEFAULT 0,
                error_message VARCHAR(1000),
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_user_id (user_id),
                INDEX idx_action (action),
                INDEX idx_module (module),
                INDEX idx_status (status),
                INDEX idx_risk_level (risk_level),
                INDEX idx_create_time (create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        addColumnIfMissing("response_result", "TEXT");
        addColumnIfMissing("status", "VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'");
        addColumnIfMissing("risk_level", "VARCHAR(20) NOT NULL DEFAULT 'LOW'");
        addColumnIfMissing("cost_time", "BIGINT DEFAULT 0");
        addIndexIfMissing("idx_status", "status");
        addIndexIfMissing("idx_risk_level", "risk_level");
    }

    private void addColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log' AND COLUMN_NAME = ?
                """, Integer.class, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE audit_log ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void addIndexIfMissing(String indexName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log' AND INDEX_NAME = ?
                """, Integer.class, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE audit_log ADD INDEX " + indexName + " (" + columnName + ")");
        }
    }
}
