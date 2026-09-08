package com.alkywallet.dto;

import java.math.BigDecimal;

public record PagoServicioResponseDTO(
        String referencia,
        String estado,
        BigDecimal monto
) {
}
