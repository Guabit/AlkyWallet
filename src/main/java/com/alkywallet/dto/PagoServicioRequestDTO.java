package com.alkywallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Usado por el mock de PedidosYa: "comercio" es solo texto libre para el ticket simulado.
public record PagoServicioRequestDTO(
        @NotBlank(message = "El comercio es obligatorio")
        String comercio,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        Double monto
) {
}
