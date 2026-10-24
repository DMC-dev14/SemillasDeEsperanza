package com.donation.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findFirstByDonation_IdOrderByTransactionDateDesc(Long donationId);
}
