package com.donation.app.dto.response;

public record DonationCreatedResponse(
    DonationResponse donation,
    String checkoutToken
) {}
