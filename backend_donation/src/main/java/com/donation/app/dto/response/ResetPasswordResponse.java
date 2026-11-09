package com.donation.app.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordResponse(
    @NotBlank(message = "El token de restablecimiento es obligatorio")
    String token,
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "La nueva contraseña debe contener al menos una letra mayúscula, una letra minúscula, un número y un carácter especial")
    String newPassword
) {
    
}
