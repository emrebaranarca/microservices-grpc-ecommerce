package com.example.order_service.messaging;

import static com.example.order_service.config.RabbitConfig.ORDER_CREATED_QUEUE;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(ORDER_CREATED_QUEUE, event);
    }
}


