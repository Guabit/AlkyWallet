package com.alkywallet.dto;

import com.alkywallet.entity.CategoriaTransaccion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GastoPorCategoriaDTO {
    private CategoriaTransaccion categoria;
    private BigDecimal total;
}
