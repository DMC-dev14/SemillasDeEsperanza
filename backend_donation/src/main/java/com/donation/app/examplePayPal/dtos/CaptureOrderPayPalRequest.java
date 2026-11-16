package com.donation.app.examplePayPal.dtos;

public record CaptureOrderPayPalRequest(
    Long donationId, 
    String checkoutToken
) {
}
