package com.alkywallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferenciaRequestDTO(
        @NotBlank(message = "El email del destinatario es obligatorio")
        @Email(message = "Debe ser un email válido")
        String destinatario,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        Double monto
) {
}