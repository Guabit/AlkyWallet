package com.alkywallet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una colocación simulada en un fondo común de inversión (FCI)
 * tipo "money market". La tasa es un valor simulado/ilustrativo: no refleja
 * una tasa de mercado real ni constituye asesoramiento financiero.
 */
@Entity
@Table(name = "inversiones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "monto_invertido", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoInvertido;

    // Tasa nominal anual SIMULADA (0.40 = 40% TNA). Es un valor de ejemplo
    // para fines demostrativos, no una cotización real de ningún FCI.
    @Column(name = "tasa_anual_nominal", nullable = false, precision = 7, scale = 5)
    @Builder.Default
    private BigDecimal tasaAnualNominal = new BigDecimal("0.40");

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @CreationTimestamp
    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_rescate")
    private LocalDateTime fechaRescate;
}
