package com.example.product_service.service.grpc;

import com.example.grpc.DiscountRequest;
import com.example.grpc.DiscountResponse;

public interface DiscountService {
    DiscountResponse getDiscount(DiscountRequest discountRequest);
}
