package com.donation.app.model;

import java.time.LocalDateTime;
import lombok.*;


import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "voucher")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false, length = 30)
    private String number;

    @Column(name = "file_path", length = 255)
    private String filePath;

    @CreationTimestamp
    @Column(name = "send_date", nullable = false, updatable = false)
    private LocalDateTime sendDate;

    @Column(name = "email_send", nullable = false, length = 100)
    private String emailSend;

    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    
}