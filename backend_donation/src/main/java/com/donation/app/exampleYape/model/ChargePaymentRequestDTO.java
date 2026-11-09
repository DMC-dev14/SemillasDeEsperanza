
package com.donation.app.exampleYape.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChargePaymentRequestDTO {

    @NotNull
    private Long donationId;

    @NotBlank
    private String token;

    @NotBlank
    private String checkoutToken;
}
