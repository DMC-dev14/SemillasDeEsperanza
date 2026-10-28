package com.donation.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
    
}
