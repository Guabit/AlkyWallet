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
 * Registro de un pago de Mercado Pago ya procesado, para no acreditar dos
 * veces el mismo pago si el webhook llega repetido (Mercado Pago reintenta
 * notificaciones que no fueron respondidas a tiempo).
 */
@Entity
@Table(name = "pagos_mercado_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoMercadoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mercado_pago_payment_id", nullable = false, unique = true)
    private String mercadoPagoPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 30)
    private String estado;

    @CreationTimestamp
    @Column(name = "fecha_procesado", nullable = false, updatable = false)
    private LocalDateTime fechaProcesado;
}
