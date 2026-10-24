package com.donation.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
    
}
