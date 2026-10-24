package com.donation.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message="El email no puede estar vacio")
    @Email(message="El formato de correo es invalido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    String email,

    @NotBlank(message="La contraseña no puede estar vacia")
    String password
){}
