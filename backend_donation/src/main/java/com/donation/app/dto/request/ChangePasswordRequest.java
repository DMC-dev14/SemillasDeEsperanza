package com.donation.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

    @NotBlank(message = "La contraseña actual no puede estar vacia")
    String currentPassword,

    @NotBlank(message = "La nueva contraseña no puede estar vacia")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 a 72 caracteres")
    @Pattern(
        regexp = "\\A(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*\\z",
        message = "La contraseña debe contener al menos una letra mayúscula, una minúscula y un número"
    )
    String newPassword

) {}
