package com.alkywallet.dto;

import com.alkywallet.entity.TipoMoneda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InversionDTO {
    private Long id;
    private TipoMoneda moneda;
    private BigDecimal montoInvertido;
    private BigDecimal tasaAnualNominal;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaRescate;
    private long diasTranscurridos;
    private BigDecimal rendimientoSimulado;
    private BigDecimal valorActual;
    private boolean activa;
}
