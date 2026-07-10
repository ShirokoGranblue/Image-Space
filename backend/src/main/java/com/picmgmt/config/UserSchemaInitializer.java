package com.picmgmt.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'users'
                  AND COLUMN_NAME = 'email_verified'
                """, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE users
                    ADD COLUMN email_verified TINYINT NOT NULL DEFAULT 0 AFTER email
                    """);
        }
    }
}
