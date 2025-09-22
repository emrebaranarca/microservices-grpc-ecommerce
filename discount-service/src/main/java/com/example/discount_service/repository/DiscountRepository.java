package com.example.discount_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.discount_service.entity.Discount;

public interface DiscountRepository extends JpaRepository<Discount,Integer> {

    Optional<Discount> findByCodeAndCategoryId(String code,Integer categoryId);

} 
