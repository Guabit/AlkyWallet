package com.alkywallet.dto;

import com.alkywallet.entity.CategoriaTransaccion;
import com.alkywallet.entity.TipoTransaccion;
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
public class TransaccionDTO {
    private Long id;
    private BigDecimal monto;
    private LocalDateTime fecha;
    private TipoTransaccion tipoTransaccion;
    private String concepto;
    private Long cuentaId;
    private CategoriaTransaccion categoria;
}
