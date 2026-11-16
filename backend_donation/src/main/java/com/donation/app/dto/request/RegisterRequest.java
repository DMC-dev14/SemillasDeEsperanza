package com.donation.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "El email no puede estar vacio")
    @Email(message = "El formato de correo es invalido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    String email,

    @NotBlank(message = "El nombre no puede estar vacio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 a 50 caracteres")
    String name,

    @NotBlank(message = "La contraseña no puede estar vacia")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 a 72 caracteres")
    @Pattern(
        regexp = "\\A(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*\\z",
        message = "La contraseña debe contener al menos una letra mayúscula, una minúscula y un número"
    )
    String password

) {}
