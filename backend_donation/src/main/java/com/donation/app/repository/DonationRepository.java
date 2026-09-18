package com.donation.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Donation;

public interface DonationRepository extends JpaRepository<Donation, Long>{
    
}
