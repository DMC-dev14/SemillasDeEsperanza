package com.donation.app.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.donation.app.enums.CampaignStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CampaignRequest(
    @NotBlank(message = "El título de la campaña es obligatorio")
    @Size(min = 3, max = 100, message = "El título debe tener entre 3 y 100 caracteres")
    String title,

    @NotBlank(message = "La descripción de la campaña es obligatoria")
    @Size(max = 255, message = "La descripción no debe superar los 255 caracteres")
    String description,

    @Size(max = 255, message = "La historia no debe superar los 255 caracteres")
    String history,

    @Size(max = 255, message = "La URL de imagen no debe superar los 255 caracteres")
    String imageUrl,

    @NotNull(message = "El monto meta es obligatorio")
    @DecimalMin(value = "100.00", message = "El monto meta debe ser al menos S/ 100.00")
    BigDecimal targetAmount,

    CampaignStatus status,

    LocalDateTime startDate,

    LocalDateTime endDate
) {}