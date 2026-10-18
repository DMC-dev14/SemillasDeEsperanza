package com.donation.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.enums.PaymentStatus;
import com.donation.app.model.Donation;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    boolean existsByDonor_Id(Long id);
    boolean existsByCampaign_Id(Long campaignId);
    long countByCampaign_Id(Long campaignId);
    long countByCampaign_IdAndStatus(Long campaignId, PaymentStatus status);
    List<Donation> findByDonor_IdOrderByDonationDateDesc(Long donorId);
    List<Donation> findAllByOrderByDonationDateDesc();
    List<Donation> findTop10ByStatusOrderByDonationDateDesc(PaymentStatus status);
    List<Donation> findByCampaign_IdAndStatus(Long campaignId, PaymentStatus status);
}