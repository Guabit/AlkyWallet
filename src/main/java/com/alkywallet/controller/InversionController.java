package com.alkywallet.controller;

import com.alkywallet.dto.InversionDTO;
import com.alkywallet.dto.InvertirRequestDTO;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.service.InversionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inversiones")
@RequiredArgsConstructor
public class InversionController {

    private final InversionService inversionService;

    @PostMapping("/invertir")
    public ResponseEntity<InversionDTO> invertir(
            @Valid @RequestBody InvertirRequestDTO request,
            Authentication authentication) {
        InversionDTO inversion = inversionService.invertir(
            authentication.getName(), 
            request.monto(),
            request.moneda()
        );
        return new ResponseEntity<>(inversion, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/rescatar")
    public ResponseEntity<InversionDTO> rescatar(
            @PathVariable Long id,
            Authentication authentication) {
        InversionDTO rescate = inversionService.rescatar(authentication.getName(), id);
        return ResponseEntity.ok(rescate);
    }

    @GetMapping
    public ResponseEntity<List<InversionDTO>> obtenerMisInversiones(
            @RequestParam(required = false) TipoMoneda moneda,
            Authentication authentication) {
        List<InversionDTO> inversiones = inversionService.obtenerPorEmail(authentication.getName(), moneda);
        return ResponseEntity.ok(inversiones);
    }
}
