package com.alkywallet.service;

import com.alkywallet.dto.CuentaDTO;
import com.alkywallet.dto.CotizacionDolarDTO;
import com.alkywallet.dto.GastoPorCategoriaDTO;
import com.alkywallet.entity.CategoriaTransaccion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Asistente de IA para consultas sobre la propia billetera. En vez de
 * dejar que el modelo decida qué datos consultar (poco confiable con
 * modelos chicos corriendo en local), el service resuelve primero la
 * intención con reglas simples y consulta los datos reales del usuario;
 * a Ollama solo se le pide que redacte la respuesta en lenguaje natural
 * a partir de ese dato ya calculado. Así la funcionalidad no depende de
 * que el modelo "alucine" un monto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsistenteIAService {

    private final TransaccionService transaccionService;
    private final CuentaService cuentaService;
    private final CotizacionService cotizacionService;
    private final OllamaClient ollamaClient;

    public String responder(String email, String pregunta) {
        String normalizada = normalizar(pregunta);

        if (contieneAlguna(normalizada, "cuanto gaste", "gastos del mes", "gasto del mes", "gasto mensual")) {
            BigDecimal total = transaccionService.obtenerTotalGastadoEsteMesPorEmail(email);
            return generarConDato(pregunta, "El usuario gastó " + formatear(total) + " este mes, sumando transferencias y pagos enviados.");
        }

        CategoriaTransaccion categoria = detectarCategoria(normalizada);
        if (categoria != null) {
            List<GastoPorCategoriaDTO> reporte = transaccionService.obtenerReporteCategoriasPorEmail(email);
            BigDecimal total = reporte.stream()
                    .filter(item -> item.getCategoria() == categoria)
                    .map(GastoPorCategoriaDTO::getTotal)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            return generarConDato(pregunta, "El usuario gastó " + formatear(total) + " en la categoría " + categoria + ".");
        }

        if (contieneAlguna(normalizada, "saldo", "cuanto tengo", "balance", "cuanta plata")) {
            CuentaDTO cuenta = cuentaService.obtenerBalancePorEmail(email);
            return generarConDato(pregunta, "El saldo disponible del usuario es " + formatear(cuenta.getSaldo()) + ".");
        }

        if (contieneAlguna(normalizada, "dolar", "cotizacion")) {
            try {
                List<CotizacionDolarDTO> cotizaciones = cotizacionService.obtenerCotizaciones();
                StringBuilder dato = new StringBuilder("Cotización actual del dólar: ");
                cotizaciones.stream().limit(2).forEach(c ->
                        dato.append(c.getNombre()).append(" compra ").append(c.getCompra())
                                .append(" venta ").append(c.getVenta()).append(". "));
                return generarConDato(pregunta, dato.toString());
            } catch (Exception ex) {
                return "No pude consultar la cotización del dólar en este momento.";
            }
        }

        return generarRespuestaGeneral(pregunta);
    }

    private String generarConDato(String preguntaOriginal, String dato) {
        String prompt = """
                Sos el asistente financiero de AlkyWallet. Respondé en español, en una sola oración \
                breve, clara y amigable, usando exclusivamente el dato provisto. No inventes números \
                ni agregues datos que no te dieron.
                Dato: %s
                Pregunta del usuario: "%s"
                Respuesta:""".formatted(dato, preguntaOriginal);
        try {
            return ollamaClient.generar(prompt).trim();
        } catch (Exception ex) {
            log.warn("Ollama no disponible, devolviendo el dato sin redactar: {}", ex.getMessage());
            // Degradación amable: la funcionalidad no se cae aunque Ollama no esté corriendo.
            return dato;
        }
    }

    private String generarRespuestaGeneral(String pregunta) {
        String prompt = """
                Sos el asistente financiero de AlkyWallet. Por ahora solo podés informar: saldo actual, \
                gastos del mes, gastos por categoría (comida, transporte, servicios, entretenimiento, \
                salud, educación) y cotización del dólar. Si la pregunta no entra en esos temas, \
                respondé amablemente que todavía no podés ayudar con eso y sugerí una de esas opciones. \
                Pregunta del usuario: "%s"
                Respuesta:""".formatted(pregunta);
        try {
            return ollamaClient.generar(prompt).trim();
        } catch (Exception ex) {
            return "Por ahora puedo contarte tu saldo, tus gastos del mes, tus gastos por categoría o la cotización del dólar. Probá preguntarme alguna de esas cosas.";
        }
    }

    private boolean contieneAlguna(String texto, String... palabras) {
        for (String palabra : palabras) {
            if (texto.contains(palabra)) return true;
        }
        return false;
    }

    private CategoriaTransaccion detectarCategoria(String texto) {
        for (CategoriaTransaccion categoria : CategoriaTransaccion.values()) {
            if (texto.contains(quitarAcentos(categoria.name().toLowerCase(Locale.ROOT)))) {
                return categoria;
            }
        }
        return null;
    }

    private String normalizar(String texto) {
        return quitarAcentos(texto.toLowerCase(Locale.ROOT));
    }

    private String quitarAcentos(String texto) {
        return texto
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u");
    }

    private String formatear(BigDecimal monto) {
        return "$" + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
