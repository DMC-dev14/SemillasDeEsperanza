package com.donation.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    
}
