package com.picmgmt.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class RabbitConfiguration {
    public static final String EXCHANGE = "image-space.events";
    public static final String IMAGE_QUEUE = "image-space.image";
    public static final String AUDIT_QUEUE = "image-space.audit";

    @Bean
    Declarables messageTopology() {
        DirectExchange exchange = new DirectExchange(EXCHANGE, true, false);
        Queue image = QueueBuilder.durable(IMAGE_QUEUE).quorum().build();
        Queue audit = QueueBuilder.durable(AUDIT_QUEUE).quorum().build();
        Queue imageDead = QueueBuilder.durable(IMAGE_QUEUE + ".dead").quorum().build();
        Queue auditDead = QueueBuilder.durable(AUDIT_QUEUE + ".dead").quorum().build();
        return new Declarables(exchange, image, audit, imageDead, auditDead,
                BindingBuilder.bind(image).to(exchange).with("image"),
                BindingBuilder.bind(audit).to(exchange).with("audit"),
                BindingBuilder.bind(imageDead).to(exchange).with("image.dead"),
                BindingBuilder.bind(auditDead).to(exchange).with("audit.dead"));
    }

    @Bean
    SimpleRabbitListenerContainerFactory reliableListenerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(true);
        return factory;
    }

    @Bean
    RabbitTemplate confirmedRabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMandatory(true);
        return template;
    }
}
