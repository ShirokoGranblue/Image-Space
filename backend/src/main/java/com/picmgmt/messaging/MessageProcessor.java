package com.picmgmt.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.audit.AuditLogEventPublisher;
import com.picmgmt.image.AsyncImageService;
import com.picmgmt.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class MessageProcessor {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final AsyncImageService images;
    private final AuditLogMapper auditMapper;
    private final AuditLogEventPublisher auditEvents;

    @Transactional(rollbackFor = Exception.class)
    public void process(String id, String token, String expectedQueueType) throws Exception {
        var rows = jdbc.queryForList("SELECT * FROM message_outbox WHERE event_id=? FOR UPDATE", id);
        if (rows.isEmpty()) return; // Already completed and expired from the retention window.
        var row = rows.getFirst();
        if (!Objects.equals(token, row.get("publish_token"))
                || !("PUBLISHING".equals(row.get("state")) || "SENT".equals(row.get("state"))
                     || "PENDING".equals(row.get("state")))) return;
        MessageEvent event = json.readValue((String) row.get("payload"), MessageEvent.class);
        if (!id.equals(event.eventId()) || event.version() != 1
                || !Objects.equals(event.type(), row.get("event_type"))
                || MessageEvent.AUDIT.equals(event.type()) != "audit".equals(expectedQueueType)) {
            throw new IllegalArgumentException("Invalid event envelope");
        }
        switch (event.type()) {
            case MessageEvent.IMAGE -> images.generate(event);
            case MessageEvent.CLEANUP -> images.cleanup(event);
            case MessageEvent.CACHE -> images.invalidate(event.imageUuid());
            case MessageEvent.AUDIT -> {
                var audit = event.audit();
                if (audit == null) throw new IllegalArgumentException("Missing audit snapshot");
                audit.setId(null);
                audit.setEventId(id);
                Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE event_id=?", Integer.class, id);
                if (count != null && count == 0) {
                    auditMapper.insert(audit);
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCommit() { auditEvents.publishIfImportant(audit); }
                    });
                }
            }
            default -> throw new IllegalArgumentException("Unknown event type");
        }
        jdbc.update("UPDATE message_outbox SET state='DONE',updated_at=CURRENT_TIMESTAMP(6),last_error=NULL WHERE event_id=?", id);
    }
}
