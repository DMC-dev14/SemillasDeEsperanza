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
public class ChargePaymentPhoneRequest {

    @JsonProperty("area_code")
    private String areaCode;

    @JsonProperty("number")
    private String number;
}
