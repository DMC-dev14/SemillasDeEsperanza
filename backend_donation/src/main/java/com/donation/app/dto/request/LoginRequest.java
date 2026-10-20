package com.donation.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
 
public record LoginRequest (
    @NotBlank(message="El email no puede estar vacio")
    @Email(message="El formato de correo es invalido")
    String email,
    @NotBlank(message="La contraseña no puede estar vacia")
    String password
){}