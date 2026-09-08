package com.alkywallet.controller;

import com.alkywallet.dto.ConversionRequestDTO;
import com.alkywallet.service.ConversionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversiones")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;

    @PostMapping
    public ResponseEntity<Map<String, String>> convertirDivisas(
            @Valid @RequestBody ConversionRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        conversionService.convertirDivisas(email, request.monedaOrigen(), request.monedaDestino(), request.monto());
        return ResponseEntity.ok(Map.of("mensaje", "Conversión realizada con éxito"));
    }
}
