package com.alkywallet.service;

import com.alkywallet.dto.CuentaDTO;
import com.alkywallet.dto.CotizacionDolarDTO;
import com.alkywallet.dto.GastoPorCategoriaDTO;
import com.alkywallet.dto.TransaccionDTO;
import com.alkywallet.entity.CategoriaTransaccion;
import com.alkywallet.entity.TipoMoneda;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Asistente de IA para consultas sobre la propia billetera. Resuelve la intención
 * consultando datos reales del usuario y le proporciona a Ollama el contexto
 * verídico para redactar la respuesta en lenguaje natural.
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

        // 1. Saldo específico en dólares (no auto-crea la cuenta si no existe)
        if (contieneAlguna(normalizada, "dolar", "dolares", "usd") &&
                contieneAlguna(normalizada, "saldo", "cuanto tengo", "balance", "plata")) {
            Optional<CuentaDTO> cuentaUsdOpt = cuentaService.obtenerCuentasPorEmail(email).stream()
                    .filter(c -> c.getTipoMoneda() == TipoMoneda.USD)
                    .findFirst();

            if (cuentaUsdOpt.isEmpty()) {
                return generarConDato(pregunta, "El usuario todavía no tiene una cuenta en dólares (USD) abierta. Puede abrir su caja de ahorro en USD gratis desde el botón en el Dashboard.");
            }
            return generarConDato(pregunta, "El saldo en dólares (USD) del usuario es " + formatearUSD(cuentaUsdOpt.get().getSaldo()) + ".");
        }

        // 2. Saldo general / en pesos
        if (contieneAlguna(normalizada, "saldo", "cuanto tengo", "balance", "cuanta plata", "mis fondos")) {
            CuentaDTO cuenta = cuentaService.obtenerBalancePorEmail(email, TipoMoneda.ARS);
            return generarConDato(pregunta, "El saldo en pesos (ARS) del usuario es " + formatear(cuenta.getSaldo()) + ".");
        }

        // 3. Gastos globales del mes
        if (contieneAlguna(normalizada, "cuanto gaste", "gastos del mes", "gasto del mes", "gasto mensual", "total gastado")) {
            BigDecimal total = transaccionService.obtenerTotalGastadoEsteMesPorEmail(email);
            return generarConDato(pregunta, "El usuario gastó " + formatear(total) + " este mes, sumando transferencias y pagos enviados.");
        }

        // 4. Mayor gasto del mes
        if (contieneAlguna(normalizada, "en que gaste mas", "mayor gasto", "gasto mas alto", "donde se fue mi plata", "donde gaste mas")) {
            List<GastoPorCategoriaDTO> reporte = transaccionService.obtenerReporteCategoriasPorEmail(email);
            var max = reporte.stream().max(Comparator.comparing(GastoPorCategoriaDTO::getTotal));
            if (max.isPresent() && max.get().getTotal().compareTo(BigDecimal.ZERO) > 0) {
                return generarConDato(pregunta, "El mayor gasto del mes fue en la categoría " + max.get().getCategoria() + " con un total de " + formatear(max.get().getTotal()) + ".");
            } else {
                return generarConDato(pregunta, "Aún no registrás gastos por categoría durante este mes.");
            }
        }

        // 5. Gastos por categoría específica
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

        // 6. Últimos movimientos / transferencias recientes
        if (contieneAlguna(normalizada, "ultimo movimiento", "ultimos movimientos", "movimientos recientes",
                "ultima transferencia", "ultimas transferencias", "mis movimientos", "que transferi", "ultimas transacciones")) {
            List<TransaccionDTO> historial = transaccionService.obtenerHistorialPorEmail(email);
            if (historial == null || historial.isEmpty()) {
                return generarConDato(pregunta, "El usuario todavía no registra ningún movimiento en su cuenta.");
            }
            TransaccionDTO ult = historial.getFirst();
            String fecha = ult.getFecha() != null ? ult.getFecha().toLocalDate().toString() : "reciente";
            String concepto = (ult.getConcepto() != null && !ult.getConcepto().isBlank()) ? ult.getConcepto() : "Operación";
            String dato = String.format("El último movimiento registrado del usuario fue un %s de %s (concepto: '%s') el %s.",
                    ult.getTipoTransaccion(), formatear(ult.getMonto()), concepto, fecha);
            return generarConDato(pregunta, dato);
        }

        // 7. Guía y ayuda operativa de la billetera
        if (contieneAlguna(normalizada, "comprar dolar", "compro dolar", "vender dolar", "comprar usd", "vender usd", "cambiar dolar")) {
            return "Podés comprar o vender dólares en la sección 'Cotización del Dólar' del Dashboard. Ingresá el monto y confirmá la operación con 'Comprar USD' o 'Vender USD' al tipo de cambio oficial.";
        }

        if (contieneAlguna(normalizada, "plazo fijo", "como invertir", "como invierto", "rendimiento", "tasa de interes", "tna")) {
            return "En la pestaña 'Inversiones' podés simular e invertir tu dinero a plazo fijo. La tasa nominal anual (TNA) estimada es del 40% en Pesos (ARS) y 4% en Dólares (USD).";
        }

        if (contieneAlguna(normalizada, "como deposito", "como ingresar dinero", "cargar dinero", "cargar plata", "como depositar")) {
            return "Para ingresar dinero, dirigite a 'Depósitos' en el menú lateral. Podés seleccionar si depositar en ARS o USD, ingresar el monto o utilizar el código QR en pantalla.";
        }

        if (contieneAlguna(normalizada, "como transfiero", "como transferir", "enviar dinero", "enviar plata", "hacer transferencia")) {
            return "Para transferir, ingresá a 'Transferencias' en el menú lateral, seleccioná la moneda (ARS o USD), escribí el email del destinatario y el monto a enviar.";
        }

        // 8. Cotización del dólar
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

        // 9. Consulta libre / general enriquecida con contexto real
        return generarRespuestaGeneralConContexto(email, pregunta);
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
            return dato;
        }
    }

    private String generarRespuestaGeneralConContexto(String email, String pregunta) {
        StringBuilder contexto = new StringBuilder();
        try {
            CuentaDTO saldoArs = cuentaService.obtenerBalancePorEmail(email, TipoMoneda.ARS);
            contexto.append("- Saldo en pesos: ").append(formatear(saldoArs.getSaldo())).append("\n");

            Optional<CuentaDTO> cuentaUsdOpt = cuentaService.obtenerCuentasPorEmail(email).stream()
                    .filter(c -> c.getTipoMoneda() == TipoMoneda.USD)
                    .findFirst();

            if (cuentaUsdOpt.isPresent()) {
                contexto.append("- Saldo en dólares: ").append(formatearUSD(cuentaUsdOpt.get().getSaldo())).append("\n");
            } else {
                contexto.append("- Cuenta en dólares: El usuario aún no ha abierto su cuenta en dólares\n");
            }

            BigDecimal totalMes = transaccionService.obtenerTotalGastadoEsteMesPorEmail(email);
            contexto.append("- Gastos de este mes: ").append(formatear(totalMes)).append("\n");

            List<TransaccionDTO> historial = transaccionService.obtenerHistorialPorEmail(email);
            if (historial != null && !historial.isEmpty()) {
                TransaccionDTO ult = historial.getFirst();
                contexto.append("- Último movimiento: ").append(ult.getTipoTransaccion()).append(" de ").append(formatear(ult.getMonto())).append("\n");
            }
        } catch (Exception e) {
            log.debug("No se pudo compilar el contexto completo del usuario: {}", e.getMessage());
        }

        String prompt = """
                Sos el asistente financiero inteligente de AlkyWallet. Respondé en español con tono amable, \
                conciso y profesional. Podés usar la siguiente información real del usuario para responder su duda \
                o guiarlo sobre la billetera:
                %s
                Pregunta del usuario: "%s"
                Respuesta:""".formatted(contexto.toString(), pregunta);

        try {
            return ollamaClient.generar(prompt).trim();
        } catch (Exception ex) {
            return "Puedo ayudarte con tu saldo en pesos o dólares, tus gastos del mes, tus últimos movimientos, la cotización del dólar, o cómo invertir y transferir en la app. ¿Qué te gustaría consultar?";
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

    private String formatearUSD(BigDecimal monto) {
        return "US$" + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
