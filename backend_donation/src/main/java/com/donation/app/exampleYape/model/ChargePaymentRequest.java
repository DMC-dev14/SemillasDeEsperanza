
package com.donation.app.exampleYape.model;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargePaymentRequest {

    private String token;

    @JsonProperty("transaction_amount")
    private BigDecimal transactionAmount;

    private String description;

    private Integer installments;

    @JsonProperty("payment_method_id")
    private String paymentMethodId;

    private ChargePaymentPayerRequest payer;

    @JsonProperty("external_reference")
    private String externalReference;
}
