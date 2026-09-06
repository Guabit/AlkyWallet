package com.alkywallet.controller;

import com.alkywallet.dto.GastoPorTipoDTO;
import com.alkywallet.dto.TransaccionDTO;
import com.alkywallet.dto.TransferenciaRequestDTO;
import com.alkywallet.service.TransaccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;

    @GetMapping("/historial")
    public ResponseEntity<List<TransaccionDTO>> getHistorial(Authentication authentication) {
        String email = authentication.getName();
        List<TransaccionDTO> historial = transaccionService.obtenerHistorialPorEmail(email);
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/reporte-gastos")
    public ResponseEntity<List<GastoPorTipoDTO>> getReporteGastos(Authentication authentication) {
        String email = authentication.getName();
        List<GastoPorTipoDTO> reporte = transaccionService.obtenerReporteGastosPorEmail(email);
        return ResponseEntity.ok(reporte);
    }

    @PostMapping("/deposito")
    public ResponseEntity<Map<String, String>> realizarDeposito(
            @RequestBody Map<String, Double> payload,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Double monto = payload.get("monto");
        transaccionService.realizarDepositoPorEmail(email, monto);
        return ResponseEntity.ok(Map.of("mensaje", "Depósito realizado con éxito"));
    }

    @PostMapping("/transferencia")
    public ResponseEntity<Map<String, String>> realizarTransferencia(
            @Valid @RequestBody TransferenciaRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        transaccionService.realizarTransferenciaPorEmail(email, request.destinatario(), request.monto());
        return ResponseEntity.ok(Map.of("mensaje", "Transferencia realizada con éxito"));
    }
}