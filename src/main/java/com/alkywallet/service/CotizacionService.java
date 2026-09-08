package com.alkywallet.service;

import com.alkywallet.dto.CotizacionDolarDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Consulta cotizaciones del dólar en Argentina desde la API pública y
 * gratuita dolarapi.com (no requiere API key). Si el servicio externo no
 * responde, se informa un error 503 en lugar de romper el resto de la app.
 *
 * Usa el RestClient.Builder autoconfigurado por Spring Boot (en vez de
 * RestClient.create()) para heredar la configuración de Jackson de la app
 * (por ejemplo, ignorar propiedades JSON desconocidas).
 */
@Service
@RequiredArgsConstructor
public class CotizacionService {

    private static final String DOLAR_API_URL = "https://dolarapi.com/v1/dolares";

    private final RestClient.Builder restClientBuilder;

    public List<CotizacionDolarDTO> obtenerCotizaciones() {
        try {
            CotizacionDolarDTO[] respuesta = restClientBuilder.build()
                    .get()
                    .uri(DOLAR_API_URL)
                    .retrieve()
                    .body(CotizacionDolarDTO[].class);

            if (respuesta == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo obtener la cotización del dólar en este momento");
            }
            return List.of(respuesta);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo obtener la cotización del dólar en este momento");
        }
    }
}
