package com.alkywallet.dto;

import com.alkywallet.entity.TipoMoneda;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ConversionRequestDTO(
        @NotNull(message = "La moneda de origen es obligatoria")
        TipoMoneda monedaOrigen,

        @NotNull(message = "La moneda de destino es obligatoria")
        TipoMoneda monedaDestino,

        @NotNull(message = "El monto a convertir es obligatorio")
        @Positive(message = "El monto a convertir debe ser mayor a cero")
        BigDecimal monto
) {}
