package com.picmgmt.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationVOTest {

    @Test
    void serializesOnlyUnambiguousUtcCreateTime() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        NotificationVO notification = new NotificationVO();
        notification.setStoredCreateTime(LocalDateTime.of(2026, 7, 16, 10, 30));
        notification.setCreateTime(Instant.parse("2026-07-16T02:30:00Z"));

        String json = mapper.writeValueAsString(notification);

        assertTrue(json.contains("\"createTime\":\"2026-07-16T02:30:00Z\""));
        assertFalse(json.contains("storedCreateTime"));
        assertFalse(json.contains("2026-07-16T10:30:00"));
    }
}
