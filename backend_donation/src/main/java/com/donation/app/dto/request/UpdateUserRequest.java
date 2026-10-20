package com.donation.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 a 50 caracteres")
    String name,

    @Email(message = "El formato de correo es invalido")
    String email,

    @Size(max = 255, message = "La URL de la imagen es demasiado larga")
    String imageUrl,

    Boolean active

) {}