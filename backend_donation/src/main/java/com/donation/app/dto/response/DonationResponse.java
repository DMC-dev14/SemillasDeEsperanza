package com.donation.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.donation.app.enums.Method;
import com.donation.app.enums.PaymentStatus;

public record DonationResponse(
    Long id,
    BigDecimal amount,
    String currency,
    String message,
    PaymentStatus status,
    LocalDateTime donationDate,
    Long donorId,
    String donorName,
    String donorEmail,
    Long campaignId,
    String campaignTitle,
    Method methodPay,
    String codeReferential,
    String voucherNumber
) {}
