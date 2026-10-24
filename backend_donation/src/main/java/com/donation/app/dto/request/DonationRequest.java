package com.donation.app.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonationRequest(
    @NotNull(message = "Debes seleccionar un proyecto o campaña")
    Long campaignId,

    @NotNull(message = "El monto de la donación es obligatorio")
    @DecimalMin(value = "5.00", message = "El monto mínimo de donación es S/ 5.00")
    BigDecimal amount,

    @NotBlank(message = "El nombre del donante es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String donorName,

    @NotBlank(message = "El correo electrónico del donante es obligatorio")
    @Email(message = "El formato de correo electrónico es inválido")
    String donorEmail,

    @Size(max = 30, message = "El teléfono es demasiado largo")
    String donorPhone,

    @NotBlank(message = "El método de pago es obligatorio")
    String paymentMethod,

    String donationType,

    @Size(max = 200, message = "El mensaje no debe superar los 200 caracteres")
    String message
) {}
