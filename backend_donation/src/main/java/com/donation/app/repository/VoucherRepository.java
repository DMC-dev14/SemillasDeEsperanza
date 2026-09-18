package com.donation.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long>{
    
}
