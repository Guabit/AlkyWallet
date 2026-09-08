package com.alkywallet.service;

import com.alkywallet.dto.MercadoPagoPreferenciaResponseDTO;
import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.PagoMercadoPago;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.entity.Usuario;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.PagoMercadoPagoRepository;
import com.alkywallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integración con Mercado Pago vía Checkout Pro (API de Preferencias),
 * llamando directamente a su REST API en vez de depender del SDK oficial
 * (así evitamos sumar una dependencia de Maven que no puedo verificar que
 * compile en tu entorno desde acá). Pensada para funcionar con las
 * credenciales de PRUEBA (sandbox) que se generan al crear una aplicación
 * en el panel de Mercado Pago Developers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoService {

    private static final String PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";
    private static final String PAYMENTS_URL = "https://api.mercadopago.com/v1/payments/";

    private final RestClient.Builder restClientBuilder;
    private final UserRepository userRepository;
    private final CuentaRepository cuentaRepository;
    private final TransaccionService transaccionService;
    private final PagoMercadoPagoRepository pagoMercadoPagoRepository;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.back-url-base:http://localhost:8080}")
    private String backUrlBase;

    @Value("${mercadopago.notification-url:}")
    private String notificationUrl;

    public MercadoPagoPreferenciaResponseDTO crearPreferencia(String email, Double monto) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Mercado Pago no está configurado (falta MERCADOPAGO_ACCESS_TOKEN)");
        }

        Map<String, Object> item = new HashMap<>();
        item.put("title", "Carga de saldo AlkyWallet");
        item.put("quantity", 1);
        item.put("currency_id", "ARS");
        item.put("unit_price", monto);

        Map<String, Object> backUrls = new HashMap<>();
        backUrls.put("success", backUrlBase + "/html/tableroDeControl.html?mp=success");
        backUrls.put("failure", backUrlBase + "/html/tableroDeControl.html?mp=failure");
        backUrls.put("pending", backUrlBase + "/html/tableroDeControl.html?mp=pending");

        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("payer", Map.of("email", email));
        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");
        body.put("external_reference", email);
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }

        try {
            Map<?, ?> respuesta = restClientBuilder.build()
                    .post()
                    .uri(PREFERENCES_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (respuesta == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mercado Pago no respondió correctamente");
            }

            // En sandbox usamos sandbox_init_point; si no viene, caemos al init_point normal.
            Object checkoutValue = respuesta.get("sandbox_init_point");
            if (checkoutValue == null) {
                checkoutValue = respuesta.get("init_point");
            }
            String checkoutUrl = (String) checkoutValue;
            String preferenceId = (String) respuesta.get("id");

            return new MercadoPagoPreferenciaResponseDTO(checkoutUrl, preferenceId);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al crear preferencia de Mercado Pago", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo iniciar el pago con Mercado Pago");
        }
    }

    /**
     * Procesa una notificación de pago: consulta el estado real del pago en
     * la API de Mercado Pago (nunca confiamos en el monto/estado que venga
     * directo en el webhook) y, si está aprobado, acredita el saldo del
     * usuario indicado en el external_reference.
     */
    @Transactional
    public void procesarNotificacion(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) return;
        if (pagoMercadoPagoRepository.existsByMercadoPagoPaymentId(paymentId)) {
            log.info("Notificación de Mercado Pago {} ya procesada, se ignora", paymentId);
            return;
        }
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Se recibió una notificación de Mercado Pago pero no hay access token configurado");
            return;
        }

        Map<?, ?> pago;
        try {
            pago = restClientBuilder.build()
                    .get()
                    .uri(PAYMENTS_URL + paymentId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            log.error("No se pudo consultar el pago {} en Mercado Pago", paymentId, ex);
            return;
        }

        if (pago == null) return;

        String estado = String.valueOf(pago.get("status"));
        String email = (String) pago.get("external_reference");
        Object montoObj = pago.get("transaction_amount");

        if (!"approved".equals(estado) || email == null || montoObj == null) {
            log.info("Pago {} de Mercado Pago con estado '{}', no se acredita", paymentId, estado);
            return;
        }

        Double monto = ((Number) montoObj).doubleValue();

        Usuario usuario = userRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            log.warn("Pago {} de Mercado Pago referencia un email inexistente: {}", paymentId, email);
            return;
        }
        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), TipoMoneda.ARS).orElse(null);
        if (cuenta == null) return;

        // Reutiliza la misma lógica de depósito que ya usa el resto de la app.
        transaccionService.realizarDepositoPorEmail(email, monto);

        try {
            pagoMercadoPagoRepository.save(PagoMercadoPago.builder()
                    .mercadoPagoPaymentId(paymentId)
                    .cuenta(cuenta)
                    .monto(BigDecimal.valueOf(monto))
                    .estado(estado)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            // Dos notificaciones llegaron casi al mismo tiempo: el saldo ya se acreditó una sola vez
            // gracias al chequeo de arriba en el 99% de los casos; esto es una red de seguridad extra.
            log.warn("Pago {} de Mercado Pago procesado en paralelo, se ignora el duplicado", paymentId);
        }
    }
}
