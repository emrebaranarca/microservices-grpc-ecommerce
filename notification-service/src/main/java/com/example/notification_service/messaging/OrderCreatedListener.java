package com.example.notification_service.messaging;

import static com.example.notification_service.config.RabbitConfig.ORDER_CREATED_QUEUE;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderCreatedListener {

    private final JavaMailSender mailSender;

    @RabbitListener(queues = ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.getCustomerEmail());
        message.setSubject("Your order #" + event.getOrderId() + " has been created");
        message.setText("Hello,\n\nYour order was created successfully. Total: " + event.getTotalPrice() +
                "\nCreated at: " + event.getCreatedAt() +
                "\n\nThanks!");
        mailSender.send(message);
    }
}


