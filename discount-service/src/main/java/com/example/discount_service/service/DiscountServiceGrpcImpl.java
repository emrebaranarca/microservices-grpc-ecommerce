package com.example.discount_service.service;

import net.devh.boot.grpc.server.service.GrpcService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.math.BigDecimal;
import java.util.Optional;

import com.example.discount_service.entity.Category;
import com.example.discount_service.entity.Discount;
import com.example.discount_service.repository.CategoryRepository;
import com.example.discount_service.repository.DiscountRepository;
import com.example.grpc.DiscountRequest;
import com.example.grpc.DiscountResponse;
import com.example.grpc.DiscountServiceGrpc;
import com.example.grpc.Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class DiscountServiceGrpcImpl extends DiscountServiceGrpc.DiscountServiceImplBase {

    private final DiscountRepository discountRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public void getDiscount(DiscountRequest request, StreamObserver<DiscountResponse> responseObserver) {
        try {
            log.debug(
                "Processing discount request for code: {} and category: {}",
                request.getCode(), 
                request.getExternalCategoryId()
            );

            Category category = findCategoryByExternalId(request.getExternalCategoryId());
            Optional<Discount> discount = discountRepository.findByCodeAndCategoryId(
                    request.getCode(), category.getId());

            DiscountResponse response = discount.isPresent()
                    ? buildValidDiscountResponse(request, discount.get())
                    : buildInvalidDiscountResponse(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing discount request: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to process discount request: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private Category  findCategoryByExternalId(long externalCategoryId) {
        return categoryRepository.findByExternalId(String.valueOf(externalCategoryId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found for external ID: " + externalCategoryId));
    }

    private DiscountResponse buildValidDiscountResponse(DiscountRequest request, Discount discount) {
        BigDecimal originalPrice = BigDecimal.valueOf(request.getPrice());
        BigDecimal discountedPrice = calculateDiscountedPrice(originalPrice, discount.getDiscountPrice());

        return DiscountResponse.newBuilder()
                .setOldPrice(request.getPrice())
                .setNewPrice(discountedPrice.floatValue())
                .setCode(request.getCode())
                .setResponse(buildSuccessResponse("Discount applied successfully"))
                .build();
    }

    private DiscountResponse buildInvalidDiscountResponse(DiscountRequest request) {
        return DiscountResponse.newBuilder()
                .setOldPrice(request.getPrice())
                .setNewPrice(request.getPrice()) // No discount applied
                .setCode(request.getCode())
                .setResponse(buildErrorResponse("Invalid discount code"))
                .build();
    }

    private BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, BigDecimal discountAmount) {
        BigDecimal newPrice = originalPrice.subtract(discountAmount);
        return newPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newPrice;
    }

    private Response buildSuccessResponse(String message) {
        return Response.newBuilder()
                .setMessage(message)
                .setStatusCode(true)
                .build();
    }

    private Response buildErrorResponse(String message) {
        return Response.newBuilder()
                .setMessage(message)
                .setStatusCode(false)
                .build();
    }
}
