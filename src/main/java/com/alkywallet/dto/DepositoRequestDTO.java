package com.alkywallet.dto;

import com.alkywallet.entity.TipoMoneda;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositoRequestDTO(
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    Double monto,
    TipoMoneda moneda
) {}
