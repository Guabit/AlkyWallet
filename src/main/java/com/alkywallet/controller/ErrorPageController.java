package com.alkywallet.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Reemplaza la "Whitelabel Error Page" de Spring Boot. Este proyecto no usa
 * un motor de plantillas (Thymeleaf/Freemarker), así que la resolución
 * automática de vistas de error no aplica: este controller intercepta
 * cualquier error/404 no manejado y responde con el 404.html propio del
 * front (para navegación normal) o con un cuerpo JSON consistente con
 * GlobalExceptionHandler (para llamadas que esperan application/json).
 */
@Controller
public class ErrorPageController implements ErrorController {

    private static final String PAGINA_404 = "static/error/404.html";

    @RequestMapping("/error")
    public ResponseEntity<?> manejarError(HttpServletRequest request) {
        int status = obtenerStatus(request);
        String accept = request.getHeader("Accept");
        boolean esperaJson = accept != null
                && accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.TEXT_HTML_VALUE);

        if (esperaJson) {
            return ResponseEntity.status(status).body(cuerpoJson(status));
        }

        try {
            ClassPathResource recurso = new ClassPathResource(PAGINA_404);
            String html = new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.status(status)
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (IOException e) {
            return ResponseEntity.status(status).body(cuerpoJson(status));
        }
    }

    private int obtenerStatus(HttpServletRequest request) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusAttr == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        try {
            return Integer.parseInt(statusAttr.toString());
        } catch (NumberFormatException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }

    private Map<String, Object> cuerpoJson(int status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("mensaje", status == 404
                ? "El recurso solicitado no existe"
                : "Ocurrió un error inesperado");
        return body;
    }
}
