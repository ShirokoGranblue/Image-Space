package com.picmgmt.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class MessageFailureService {
    private final JdbcTemplate jdbc;

    public boolean waitingForDeadLetter(String id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM message_outbox WHERE event_id=? AND state IN ('DEAD_PENDING','DEAD_PUBLISHING')", Integer.class, id);
        return count != null && count > 0;
    }

    @Transactional
    public void failed(String id, String token, Exception error) {
        // Scheduling the retry/dead-letter is durable before the original delivery is acknowledged.
        // The publisher subsequently confirms the replacement delivery (including dead letters).
        int changed = jdbc.update("""
            UPDATE message_outbox SET
                state=CASE WHEN failures>=3 OR ? THEN 'DEAD_PENDING' ELSE 'PENDING' END,
                next_attempt_at=TIMESTAMPADD(SECOND,POWER(2,failures+1),CURRENT_TIMESTAMP(6)),
                failures=failures+1,publish_token=NULL,last_error=?,updated_at=CURRENT_TIMESTAMP(6)
            WHERE event_id=? AND publish_token=? AND state IN ('PENDING','PUBLISHING','SENT')
            """, error instanceof IllegalArgumentException, error.getClass().getSimpleName(), id, token);
        if (changed > 0) log.warn("Message processing failed: event={}, error={}", id, error.getClass().getSimpleName());
    }
}
