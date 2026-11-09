
package com.donation.app.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonationRequest(

    @NotNull(message = "Debes seleccionar un proyecto o campaña")
    Long campaignId,

    @NotNull(message = "El monto de la donación es obligatorio")
    @DecimalMin(
        value = "1.00",
        message = "El monto mínimo de donación es S/ 1.00"
    )
    @Digits(
        integer = 8,
        fraction = 2,
        message = "El monto debe tener máximo dos decimales"
    )
    BigDecimal amount,

    @NotBlank(message = "El nombre del donante es obligatorio")
    @Size(min = 2, max = 100)
    String donorName,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico es inválido")
    String donorEmail,

    @Size(max = 30)
    String donorPhone,

    @NotBlank(message = "El método de pago es obligatorio")
    String paymentMethod,

    String donationType,

    @Size(max = 200)
    String message

) {}
