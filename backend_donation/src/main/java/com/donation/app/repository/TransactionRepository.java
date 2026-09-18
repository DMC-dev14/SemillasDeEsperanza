package com.donation.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{
    
}
