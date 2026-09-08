package com.alkywallet.service;

import com.alkywallet.dto.CotizacionDolarDTO;
import com.alkywallet.entity.TipoMoneda;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DolarApiExchangeRateProvider implements ExchangeRateProvider {

    private final CotizacionService cotizacionService;

    @Override
    public BigDecimal getExchangeRate(TipoMoneda from, TipoMoneda to) {
        if (from == to) {
            return BigDecimal.ONE;
        }

        List<CotizacionDolarDTO> cotizaciones = cotizacionService.obtenerCotizaciones();
        
        // We look for "oficial" rate
        CotizacionDolarDTO oficial = cotizaciones.stream()
                .filter(c -> "oficial".equalsIgnoreCase(c.getCasa()))
                .findFirst()
                .orElse(cotizaciones.getFirst());

        if (from == TipoMoneda.USD && to == TipoMoneda.ARS) {
            // User sells USD to get ARS: we use the "compra" rate
            return oficial.getCompra();
        } else if (from == TipoMoneda.ARS && to == TipoMoneda.USD) {
            // User buys USD with ARS: we use "venta" rate.
            // The exchange rate is 1 / venta (how many dollars per ARS)
            return BigDecimal.ONE.divide(oficial.getVenta(), 6, RoundingMode.HALF_EVEN);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversión no soportada entre " + from + " y " + to);
    }
}
