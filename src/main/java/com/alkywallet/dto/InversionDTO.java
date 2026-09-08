package com.alkywallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Estado de una inversión simulada, incluyendo el rendimiento acumulado
 * a la fecha de la consulta (calculado al vuelo, no persistido).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InversionDTO {
    private Long id;
    private BigDecimal montoInvertido;
    private BigDecimal tasaAnualNominal;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaRescate;
    private long diasTranscurridos;
    private BigDecimal rendimientoSimulado;
    private BigDecimal valorActual;
    private boolean activa;
}
