package com.alkywallet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un registro histórico e inmutable de un movimiento sobre una Cuenta.
 * Una vez creada, no puede modificarse (sin setters públicos para sus campos).
 */
@Entity
@Table(
    name = "transacciones",
    indexes = {
        @Index(name = "idx_cuenta_fecha", columnList = "cuenta_id, fecha")
    }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // requerido por Hibernate, sin uso externo
@AllArgsConstructor(access = AccessLevel.PRIVATE)  // solo accesible vía @Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransaccion tipo;

    @Column(nullable = false)
    private String concepto;

    // Categoría de gasto/ingreso (comida, transporte, inversión, etc.).
    // Nueva columna, no rompe transacciones existentes: por defecto OTROS.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CategoriaTransaccion categoria = CategoriaTransaccion.OTROS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false, updatable = false)
    private Cuenta cuenta;
}
