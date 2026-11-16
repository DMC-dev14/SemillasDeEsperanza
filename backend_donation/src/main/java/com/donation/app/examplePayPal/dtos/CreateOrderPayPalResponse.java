package com.donation.app.examplePayPal.dtos;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Response payload for creating a PayPal order")
public class CreateOrderPayPalResponse {

    @Schema(description = "Orden del id")
    private String orderId;

    @Schema(description = "Estado de la paga")
    private String status;

    @Schema(description = "link de la aprobacion")
    private String approvalUrl;

    @Schema(description = "Monto a cobrar en USD")
    private BigDecimal amountUsd;
}