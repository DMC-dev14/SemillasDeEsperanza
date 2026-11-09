package com.donation.app.exampleYape.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YapeGenerateTokenRequest {
    private String phoneNumber;
    private String otp;
    private String requestId;   // ✅ antes: request_id
}