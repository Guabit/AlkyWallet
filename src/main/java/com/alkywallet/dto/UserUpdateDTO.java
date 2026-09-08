package com.alkywallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    String nombre,
    @Size(max = 50, message = "El apellido no puede exceder 50 caracteres")
    String apellido,
    @Email(message = "El email debe tener un formato válido")
    String email
) {}
