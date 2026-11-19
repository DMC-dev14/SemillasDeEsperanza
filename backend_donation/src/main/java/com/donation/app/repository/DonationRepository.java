package com.donation.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.donation.app.enums.PaymentStatus;
import com.donation.app.model.Donation;

import jakarta.persistence.LockModeType;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    boolean existsByDonor_Id(Long id);

    boolean existsByCampaign_Id(Long campaignId);

    long countByCampaign_Id(Long campaignId);

    long countByCampaign_IdAndStatus(Long campaignId, PaymentStatus status);

    List<Donation> findByDonor_IdOrderByDonationDateDesc(Long donorId);

    Page<Donation> findByDonor_Id(Long donorId, Pageable pageable);

    List<Donation> findAllByOrderByDonationDateDesc();

    List<Donation> findTop10ByStatusOrderByDonationDateDesc(PaymentStatus status);

    List<Donation> findByCampaign_IdAndStatus(Long campaignId, PaymentStatus status);

    Optional<Donation> findByPaypalOrderId(String paypalOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Donation d WHERE d.id = :id")
    Optional<Donation> findLockedById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Donation d SET d.paymentAttemptKey = :attemptKey WHERE d.id = :donationId")
    int updatePaymentAttemptKey(
            @Param("donationId") Long donationId,
            @Param("attemptKey") String attemptKey);
}