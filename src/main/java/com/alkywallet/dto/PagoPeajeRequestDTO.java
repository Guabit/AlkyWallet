package com.alkywallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Usado por el mock de Telepase. "patente" es opcional, solo para el ticket simulado.
public record PagoPeajeRequestDTO(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        Double monto,

        String patente
) {
}
