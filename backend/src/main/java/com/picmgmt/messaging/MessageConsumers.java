package com.picmgmt.messaging;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class MessageConsumers {
    private final MessageProcessor processor;
    private final MessageFailureService failures;
    private final OutboxPublisher publisher;

    @RabbitListener(queues = RabbitConfiguration.IMAGE_QUEUE, containerFactory = "reliableListenerFactory")
    public void image(Message message, Channel channel) throws IOException { consume(message, channel, "image"); }

    @RabbitListener(queues = RabbitConfiguration.AUDIT_QUEUE, containerFactory = "reliableListenerFactory")
    public void audit(Message message, Channel channel) throws IOException { consume(message, channel, "audit"); }

    private void consume(Message message, Channel channel, String type) throws IOException {
        String id = message.getMessageProperties().getMessageId();
        String token = message.getMessageProperties().getHeader("dispatch-token");
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            if (id == null || token == null) throw new IllegalArgumentException("Missing dispatch identity");
            processor.process(id, token, type);
        } catch (Exception error) {
            if (id == null || token == null) {
                // Unknown deliveries cannot be safely associated with an outbox record.
                channel.basicNack(tag, false, true);
                java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.SECONDS.toNanos(1));
                return;
            }
            try {
                failures.failed(id, token, error);
            } catch (RuntimeException databaseUnavailable) {
                channel.basicNack(tag, false, true);
                java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.SECONDS.toNanos(1));
                return;
            }
        }
        if (failures.waitingForDeadLetter(id)) {
            publisher.publishDue();
            if (failures.waitingForDeadLetter(id)) {
                // Preserve the original delivery until the dead-letter copy is confirmed.
                channel.basicNack(tag, false, true);
                java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.SECONDS.toNanos(1));
                return;
            }
        }
        channel.basicAck(tag, false);
    }
}
