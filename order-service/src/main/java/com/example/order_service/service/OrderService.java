package com.example.order_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.grpc.DiscountRequest;
import com.example.grpc.DiscountResponse;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderItem;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.grpc.DiscountService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DiscountService discountService;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order createOrder(Order order, String discountCode, Long categoryExternalId) {
        order.setCreatedAt(Instant.now());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setLineTotal(lineTotal);
            item.setOrder(order);
            total = total.add(lineTotal);
        }

        if (discountCode != null && !discountCode.isBlank()) {
            DiscountRequest request = DiscountRequest.newBuilder()
                    .setCode(discountCode)
                    .setPrice(total.floatValue())
                    .setExternalCategoryId(categoryExternalId != null ? categoryExternalId : 0L)
                    .build();
            DiscountResponse response = discountService.getDiscount(request);
            if (response.getResponse().getStatusCode()) {
                total = BigDecimal.valueOf(response.getNewPrice());
            }
        }

        order.setTotalPrice(total);
        return orderRepository.save(order);
    }
}


