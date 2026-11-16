package com.donation.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 a 50 caracteres")
    String name,

    @Email(message = "El formato de correo es invalido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    String email,

    @Pattern(regexp = "^(USUARIO|ADMINISTRADOR)$", message = "El rol debe ser USUARIO o ADMINISTRADOR")
    String rol,

    Boolean active

) {}
