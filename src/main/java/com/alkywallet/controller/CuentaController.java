package com.alkywallet.controller;

import com.alkywallet.dto.CuentaDTO;
import com.alkywallet.dto.CrearCuentaDTO;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping
    public ResponseEntity<List<CuentaDTO>> getCuentas(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(cuentaService.obtenerCuentasPorEmail(email));
    }

    @GetMapping("/balance")
    public ResponseEntity<CuentaDTO> getBalance(Authentication authentication, @RequestParam(required = false, defaultValue = "ARS") TipoMoneda moneda) {
        String email = authentication.getName();
        CuentaDTO balance = cuentaService.obtenerBalancePorEmail(email, moneda);
        return ResponseEntity.ok(balance);
    }
    
    @PostMapping
    public ResponseEntity<CuentaDTO> crearCuenta(Authentication authentication, @jakarta.validation.Valid @RequestBody CrearCuentaDTO request) {
        String email = authentication.getName();
        CuentaDTO cuenta = cuentaService.crearCuenta(email, request.moneda());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(cuenta);
    }
}
