package com.donation.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.donation.app.enums.CampaignStatus;
import com.donation.app.model.Campaign;

import jakarta.persistence.LockModeType;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatusOrderByCreateDateDesc(CampaignStatus status);

    Optional<Campaign> findFirstByStatusOrderByAmountCollectedDesc(CampaignStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campaign c WHERE c.id = :id")
    Optional<Campaign> findLockedById(@Param("id") Long id);
}
