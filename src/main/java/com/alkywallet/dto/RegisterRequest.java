package com.alkywallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "El dni es obligatorio")
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9]{7,8}$", message = "El DNI debe contener entre 7 y 8 dígitos")
    String dni,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    String password
){}
