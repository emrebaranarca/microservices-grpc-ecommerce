package com.example.product_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grpc.DiscountRequest;
import com.example.grpc.DiscountResponse;
import com.example.product_service.dto.DiscountRequestDto;
import com.example.product_service.dto.DiscountResponseDto;
import com.example.product_service.service.grpc.DiscountService;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<DiscountResponseDto> calculateDiscount(@RequestBody DiscountRequestDto requestDto) {
        // Convert DTO to gRPC request
        DiscountRequest grpcRequest = DiscountRequest.newBuilder()
                .setCode(requestDto.getCode())
                .setPrice(requestDto.getPrice())
                .setExternalCategoryId(requestDto.getExternalCategoryId())
                .build();

        // Call gRPC service
        DiscountResponse grpcResponse = discountService.getDiscount(grpcRequest);

        // Convert gRPC response to DTO
        DiscountResponseDto responseDto = DiscountResponseDto.builder()
                .code(grpcResponse.getCode())
                .newPrice(grpcResponse.getNewPrice())
                .oldPrice(grpcResponse.getOldPrice())
                .response(DiscountResponseDto.ResponseDto.builder()
                        .statusCode(grpcResponse.getResponse().getStatusCode())
                        .message(grpcResponse.getResponse().getMessage())
                        .build())
                .build();

        return ResponseEntity.ok(responseDto);
    }
}
