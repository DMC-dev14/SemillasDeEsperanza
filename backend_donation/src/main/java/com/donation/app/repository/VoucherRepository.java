package com.donation.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findFirstByTransaction_Id(Long transactionId);
}
