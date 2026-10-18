package com.donation.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.donation.app.enums.CampaignStatus;

public record CampaignResponse(
    Long id,
    String title,
    String description,
    String history,
    String imageUrl,
    String category,
    String badgeClass,
    String location,
    BigDecimal targetAmount,
    BigDecimal amountCollected,
    int porcentaje,
    long donorsCount,
    CampaignStatus status,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime createDate,
    LocalDateTime updateDate
) {}