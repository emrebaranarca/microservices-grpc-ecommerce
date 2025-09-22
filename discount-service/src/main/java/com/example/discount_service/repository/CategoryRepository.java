package com.example.discount_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.discount_service.entity.Category;

public interface CategoryRepository extends JpaRepository<Category,Integer> {

    Optional<Category> findByExternalId(String externalId);
    
}
