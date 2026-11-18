package com.donation.app.exampleYape.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargePaymentAdditionalInfo {

    @JsonProperty("ip_address")
    private String ipAddress;
}
