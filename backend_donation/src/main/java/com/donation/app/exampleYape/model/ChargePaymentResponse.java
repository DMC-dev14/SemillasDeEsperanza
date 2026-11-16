
package com.donation.app.exampleYape.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChargePaymentResponse {

    private String id;

    private String status;

    @JsonProperty("status_detail")
    private String statusDetail;

    @JsonProperty("transaction_amount")
    private String transactionAmount;
}
