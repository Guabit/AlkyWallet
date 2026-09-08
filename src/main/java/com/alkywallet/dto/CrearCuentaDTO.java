package com.alkywallet.dto;

import com.alkywallet.entity.TipoMoneda;
import jakarta.validation.constraints.NotNull;

public record CrearCuentaDTO(
    @NotNull(message = "La moneda es obligatoria") TipoMoneda moneda
) {}
