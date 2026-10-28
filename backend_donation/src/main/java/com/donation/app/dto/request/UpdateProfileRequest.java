package com.donation.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

    @NotBlank(message = "El nombre no puede estar vacio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 a 50 caracteres")
    String name,

    @NotBlank(message = "El email no puede estar vacio")
    @Email(message = "El formato de correo es invalido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    String email

) {}
