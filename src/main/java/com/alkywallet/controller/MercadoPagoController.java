package com.alkywallet.controller;

import com.alkywallet.dto.MercadoPagoPreferenciaRequestDTO;
import com.alkywallet.dto.MercadoPagoPreferenciaResponseDTO;
import com.alkywallet.service.MercadoPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mercadopago")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/preferencia")
    public ResponseEntity<MercadoPagoPreferenciaResponseDTO> crearPreferencia(
            @Valid @RequestBody MercadoPagoPreferenciaRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(mercadoPagoService.crearPreferencia(authentication.getName(), request.monto()));
    }

    // Mercado Pago puede notificar por POST (webhooks nuevos) o por GET (IPN clásico),
    // así que aceptamos los dos. Este endpoint es público: lo llama Mercado Pago, no un
    // usuario logueado (ver SecurityConfig).
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhookPost(
            @RequestParam Map<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        procesarSinRomper(extraerPaymentId(queryParams, body));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/webhook")
    public ResponseEntity<Void> webhookGet(@RequestParam Map<String, String> queryParams) {
        procesarSinRomper(extraerPaymentId(queryParams, null));
        return ResponseEntity.ok().build();
    }

    private void procesarSinRomper(String paymentId) {
        try {
            mercadoPagoService.procesarNotificacion(paymentId);
        } catch (Exception ex) {
            // Nunca devolvemos error a Mercado Pago por una falla nuestra: eso solo
            // provoca reintentos. Se registra el error para revisarlo manualmente.
            log.error("Error procesando notificación de Mercado Pago", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String extraerPaymentId(Map<String, String> queryParams, Map<String, Object> body) {
        if (queryParams.containsKey("data.id")) {
            return queryParams.get("data.id");
        }
        if ("payment".equals(queryParams.get("type")) && queryParams.containsKey("id")) {
            return queryParams.get("id");
        }
        if ("payment".equals(queryParams.get("topic")) && queryParams.containsKey("id")) {
            return queryParams.get("id");
        }
        if (body != null && body.get("data") instanceof Map) {
            Object id = ((Map<String, Object>) body.get("data")).get("id");
            return id != null ? id.toString() : null;
        }
        return null;
    }
}
