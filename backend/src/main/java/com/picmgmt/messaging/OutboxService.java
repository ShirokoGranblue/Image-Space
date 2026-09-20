package com.picmgmt.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class OutboxService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    @Transactional(propagation = Propagation.MANDATORY)
    public void image(String uuid, String originalKey) {
        insert(new MessageEvent(UUID.randomUUID().toString(), 1, MessageEvent.IMAGE, uuid, originalKey, null, null));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void audit(AuditLog audit) {
        String id = UUID.randomUUID().toString();
        audit.setEventId(id);
        insert(new MessageEvent(id, 1, MessageEvent.AUDIT, null, null, audit, null));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void invalidateImage(String uuid) {
        insert(new MessageEvent(UUID.randomUUID().toString(), 1, MessageEvent.CACHE, uuid, null, null, null));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanup(String uuid, List<String> keys) {
        if (!keys.isEmpty()) insert(new MessageEvent(UUID.randomUUID().toString(), 1,
                MessageEvent.CLEANUP, uuid, null, null, List.copyOf(keys)));
    }

    private void insert(MessageEvent event) {
        try {
            jdbc.update("INSERT INTO message_outbox(event_id,event_type,payload) VALUES (?,?,?)",
                    event.eventId(), event.type(), json.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize message event", e);
        }
    }
}
