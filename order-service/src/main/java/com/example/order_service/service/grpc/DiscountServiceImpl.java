package com.example.order_service.service.grpc;

import org.springframework.stereotype.Service;

import com.example.grpc.DiscountRequest;
import com.example.grpc.DiscountResponse;
import com.example.grpc.DiscountServiceGrpc;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@Service
public class DiscountServiceImpl implements DiscountService {

    @GrpcClient("discount-service")
    private DiscountServiceGrpc.DiscountServiceBlockingStub discountServiceStub;

    @Override
    public DiscountResponse getDiscount(DiscountRequest discountRequest) {
        log.debug("Calling discount service with request: {}", discountRequest);
        return discountServiceStub.getDiscount(discountRequest);
    }
}


