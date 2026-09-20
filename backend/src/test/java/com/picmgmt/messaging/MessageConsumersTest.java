package com.picmgmt.messaging;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import static org.mockito.Mockito.*;

class MessageConsumersTest {
    @Test void failedDeadLetterPublicationDoesNotAcknowledgeOriginal() throws Exception {
        MessageProcessor processor = mock(MessageProcessor.class);
        MessageFailureService failures = mock(MessageFailureService.class);
        OutboxPublisher publisher = mock(OutboxPublisher.class);
        Channel channel = mock(Channel.class);
        when(failures.waitingForDeadLetter("id")).thenReturn(true);
        var message = MessageBuilder.withBody(new byte[0]).setMessageId("id")
                .setHeader("dispatch-token", "token").setDeliveryTag(7L).build();
        new MessageConsumers(processor, failures, publisher).image(message, channel);
        verify(publisher).publishDue();
        verify(channel).basicNack(7, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test void databaseFailureRetainsOriginalDelivery() throws Exception {
        MessageProcessor processor = mock(MessageProcessor.class);
        MessageFailureService failures = mock(MessageFailureService.class);
        Channel channel = mock(Channel.class);
        doThrow(new IllegalStateException()).when(processor).process("id", "token", "audit");
        doThrow(new IllegalStateException()).when(failures).failed(eq("id"), eq("token"), any());
        var message = MessageBuilder.withBody(new byte[0]).setMessageId("id")
                .setHeader("dispatch-token", "token").setDeliveryTag(3L).build();
        new MessageConsumers(processor, failures, mock(OutboxPublisher.class)).audit(message, channel);
        verify(channel).basicNack(3, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test void successAcknowledgesOnlyAfterProcessingReturns() throws Exception {
        MessageProcessor processor = mock(MessageProcessor.class);
        MessageFailureService failures = mock(MessageFailureService.class);
        Channel channel = mock(Channel.class);
        var message = MessageBuilder.withBody(new byte[0]).setMessageId("id")
                .setHeader("dispatch-token", "token").setDeliveryTag(2L).build();
        new MessageConsumers(processor, failures, mock(OutboxPublisher.class)).audit(message, channel);
        var order = inOrder(processor, channel);
        order.verify(processor).process("id", "token", "audit");
        order.verify(channel).basicAck(2, false);
    }
}
