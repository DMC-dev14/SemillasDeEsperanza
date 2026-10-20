package com.donation.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.enums.CampaignStatus;
import com.donation.app.model.Campaign;

public interface CampaignRepository  extends JpaRepository <Campaign, Long>{

    List<Campaign> findByStatusOrderByCreateDateDesc(CampaignStatus status);

    Optional<Campaign> findFirstByStatusOrderByAmountCollectedDesc(CampaignStatus status);
}
