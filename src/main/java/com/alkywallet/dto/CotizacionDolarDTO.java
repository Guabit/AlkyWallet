package com.alkywallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Cotización de una casa de cambio (oficial, blue, etc.), en el mismo
 * formato que expone dolarapi.com. @JsonIgnoreProperties protege la
 * deserialización si la API externa agrega campos nuevos (por ejemplo
 * "moneda") que no necesitamos mapear acá.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CotizacionDolarDTO {
    private String casa;
    private String nombre;
    private BigDecimal compra;
    private BigDecimal venta;
    private String fechaActualizacion;
}
