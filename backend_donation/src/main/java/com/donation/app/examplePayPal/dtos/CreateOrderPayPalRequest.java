package com.donation.app.examplePayPal.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request para crear una orden de PayPal")
public record CreateOrderPayPalRequest(

        @NotNull(message = "El ID de la donación es obligatorio")
        @Schema(description = "ID de la donación pendiente", example = "1")
        Long donationId,

        @NotBlank(message = "El token de checkout es obligatorio")
        @Schema(description = "Token devuelto por POST /api/donations")
        String checkoutToken

) {
}