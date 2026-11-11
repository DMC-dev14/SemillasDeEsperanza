package com.donation.app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.donation.app.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "donation")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "currency", length = 3)
    private String currency = "PEN";

    @Column(name = "message", length = 255)
    private String message;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @CreationTimestamp
    @Column(name = "donation_date", nullable = false, updatable = false)
    private LocalDateTime donationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "payer_email", length = 255)
    private String payerEmail;

    @Column(name = "mercadopago_payment_id", length = 64, unique = true)
    private String mercadoPagoPaymentId;

    @Column(name = "payment_attempt_key", length = 36, unique = true)
    private String paymentAttemptKey;

    @Column(name = "checkout_token", length = 36, unique = true)
    private String checkoutToken;

    @Column(name = "paypal_order_id", length = 64, unique = true)
    private String paypalOrderId;

    @Column(name = "paypal_capture_id", length = 64, unique = true)
    private String paypalCaptureId;

    @Column(name = "paypal_amount_usd", precision = 10, scale = 2)
    private BigDecimal paypalAmountUsd;

    @Column(name = "paypal_exchange_rate", precision = 10, scale = 4)
    private BigDecimal paypalExchangeRate;

}