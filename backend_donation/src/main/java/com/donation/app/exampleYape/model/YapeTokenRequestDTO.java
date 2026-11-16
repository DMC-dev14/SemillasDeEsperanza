package com.donation.app.exampleYape.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class YapeTokenRequestDTO{

    @NotNull(message = "La donación es obligatoria")
    private Long donationId;

    @NotBlank(message = "El checkoutToken es obligatorio")
    private String checkoutToken;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "\\d{9}", message = "El celular debe tener 9 dígitos")
    private String phoneNumber;

    @NotBlank(message = "El código de aprobación es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos")
    private String otp;
}