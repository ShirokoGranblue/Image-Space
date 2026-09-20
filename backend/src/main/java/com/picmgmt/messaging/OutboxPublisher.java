package com.picmgmt.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class OutboxPublisher {
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;

    @Scheduled(fixedDelayString = "${messaging.publish-delay-ms:1000}")
    public synchronized void publishDue() {
        // A crashed publisher's lease expires; SENT events remain owned by RabbitMQ.
        jdbc.update("""
            UPDATE message_outbox SET state=CASE WHEN state='DEAD_PUBLISHING' THEN 'DEAD_PENDING' ELSE 'PENDING' END
            WHERE state IN ('PUBLISHING','DEAD_PUBLISHING') AND next_attempt_at<=CURRENT_TIMESTAMP(6)
            """);
        var events = jdbc.queryForList("""
            SELECT event_id,event_type,payload,state FROM message_outbox
            WHERE state IN ('PENDING','DEAD_PENDING') AND next_attempt_at<=CURRENT_TIMESTAMP(6)
            ORDER BY next_attempt_at,created_at LIMIT 20
            """);
        for (var event : events) {
            String id = (String) event.get("event_id");
            String state = (String) event.get("state");
            boolean dead = "DEAD_PENDING".equals(state);
            String publishing = dead ? "DEAD_PUBLISHING" : "PUBLISHING";
            String token = UUID.randomUUID().toString();
            int claimed = jdbc.update("""
                UPDATE message_outbox SET state=?,publish_token=?,publish_attempts=publish_attempts+1,
                    next_attempt_at=TIMESTAMPADD(SECOND,60,CURRENT_TIMESTAMP(6)),updated_at=CURRENT_TIMESTAMP(6)
                WHERE event_id=? AND state=? AND next_attempt_at<=CURRENT_TIMESTAMP(6)
                """, publishing, token, id, state);
            if (claimed == 0) continue;
            String route = MessageEvent.AUDIT.equals(event.get("event_type")) ? "audit" : "image";
            if (dead) route += ".dead";
            try {
                CorrelationData correlation = new CorrelationData(token);
                var message = MessageBuilder.withBody(((String) event.get("payload")).getBytes(StandardCharsets.UTF_8))
                        .setContentType("application/json").setMessageId(id)
                        .setHeader("dispatch-token", token).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
                rabbit.send(RabbitConfiguration.EXCHANGE, route, message, correlation);
                var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("Message was not routed and confirmed");
                }
                // A fast consumer may already have finished or scheduled a retry. Never overwrite it.
                jdbc.update("""
                    UPDATE message_outbox SET state=?,updated_at=CURRENT_TIMESTAMP(6),last_error=NULL
                    WHERE event_id=? AND state=? AND publish_token=?
                    """, dead ? "DEAD" : "SENT", id, publishing, token);
            } catch (Exception failure) {
                if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                jdbc.update("""
                    UPDATE message_outbox SET state=?,next_attempt_at=TIMESTAMPADD(SECOND,
                        LEAST(300,POWER(2,LEAST(publish_attempts,8))),CURRENT_TIMESTAMP(6)),
                        updated_at=CURRENT_TIMESTAMP(6),last_error=?
                    WHERE event_id=? AND state=? AND publish_token=?
                    """, state, failure.getClass().getSimpleName(), id, publishing, token);
                log.warn("Message publication deferred: event={}, error={}", id, failure.getClass().getSimpleName());
                break; // Do not spend one connection timeout per pending event during an outage.
            }
        }
    }

    @Scheduled(fixedDelayString = "${messaging.monitor-delay-ms:60000}")
    public void monitor() {
        var counts = jdbc.queryForList("SELECT state,COUNT(*) AS total FROM message_outbox WHERE state<>'DONE' GROUP BY state");
        Long age = jdbc.queryForObject("""
            SELECT COALESCE(TIMESTAMPDIFF(SECOND,MIN(created_at),CURRENT_TIMESTAMP),0)
            FROM message_outbox WHERE state<>'DONE'
            """, Long.class);
        log.info("Message backlog: states={}, oldestSeconds={}", counts, age);
        for (String queue : new String[]{RabbitConfiguration.IMAGE_QUEUE + ".dead", RabbitConfiguration.AUDIT_QUEUE + ".dead"}) {
            try {
                Long count = rabbit.execute(channel -> channel.queueDeclarePassive(queue).getMessageCount() * 1L);
                log.info("Message dead letters: queue={}, total={}", queue, count);
            } catch (RuntimeException e) {
                log.warn("Cannot inspect dead-letter queue: {}", queue);
                break;
            }
        }
        jdbc.update("DELETE FROM message_outbox WHERE state='DONE' AND updated_at<TIMESTAMPADD(DAY,-7,CURRENT_TIMESTAMP) LIMIT 1000");
    }
}
