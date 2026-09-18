package com.donation.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Campaign;

public interface CampaignRepository  extends JpaRepository <Campaign, Long>{
    
}
