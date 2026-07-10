package com.picmgmt.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSchemaInitializerTest {

    @Test
    void addsEmailVerifiedColumnOnlyWhenMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(contains("email_verified"), eq(Integer.class)))
                .thenReturn(0);

        new UserSchemaInitializer(jdbcTemplate).run();

        verify(jdbcTemplate).execute(contains("ADD COLUMN email_verified"));
    }

    @Test
    void leavesExistingColumnUntouched() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(contains("email_verified"), eq(Integer.class)))
                .thenReturn(1);

        new UserSchemaInitializer(jdbcTemplate).run();

        verify(jdbcTemplate, never()).execute(contains("ADD COLUMN email_verified"));
    }
}
