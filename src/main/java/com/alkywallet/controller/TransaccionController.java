package com.alkywallet.controller;

import com.alkywallet.dto.GastoPorCategoriaDTO;
import com.alkywallet.dto.GastoPorTipoDTO;
import com.alkywallet.dto.TransaccionDTO;
import com.alkywallet.dto.TransferenciaRequestDTO;
import com.alkywallet.dto.DepositoRequestDTO;
import com.alkywallet.entity.TipoMoneda;
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
    public ResponseEntity<List<TransaccionDTO>> getHistorial(Authentication authentication, @RequestParam(required = false, defaultValue = "ARS") TipoMoneda moneda) {
        String email = authentication.getName();
        List<TransaccionDTO> historial = transaccionService.obtenerHistorialPorEmail(email, moneda);
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/reporte-gastos")
    public ResponseEntity<List<GastoPorTipoDTO>> getReporteGastos(Authentication authentication, @RequestParam(required = false, defaultValue = "ARS") TipoMoneda moneda) {
        String email = authentication.getName();
        List<GastoPorTipoDTO> reporte = transaccionService.obtenerReporteGastosPorEmail(email, moneda);
        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/reporte-categorias")
    public ResponseEntity<List<GastoPorCategoriaDTO>> getReporteCategorias(Authentication authentication, @RequestParam(required = false, defaultValue = "ARS") TipoMoneda moneda) {
        String email = authentication.getName();
        List<GastoPorCategoriaDTO> reporte = transaccionService.obtenerReporteCategoriasPorEmail(email, moneda);
        return ResponseEntity.ok(reporte);
    }

    @PostMapping("/deposito")
    public ResponseEntity<Map<String, String>> realizarDeposito(
            @jakarta.validation.Valid @RequestBody DepositoRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        TipoMoneda moneda = request.moneda() != null ? request.moneda() : TipoMoneda.ARS;
        
        transaccionService.realizarDepositoPorEmail(email, request.monto(), moneda);
        return ResponseEntity.ok(Map.of("mensaje", "Depósito realizado con éxito"));
    }

    @PostMapping("/transferencia")
    public ResponseEntity<Map<String, String>> realizarTransferencia(
            @Valid @RequestBody TransferenciaRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        TipoMoneda moneda = request.moneda() != null ? request.moneda() : TipoMoneda.ARS;
        transaccionService.realizarTransferenciaPorEmail(email, request.destinatario(), request.monto(), request.categoria(), moneda);
        return ResponseEntity.ok(Map.of("mensaje", "Transferencia realizada con éxito"));
    }
}
