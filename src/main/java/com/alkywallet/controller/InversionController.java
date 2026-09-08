package com.alkywallet.controller;

import com.alkywallet.dto.InversionDTO;
import com.alkywallet.dto.InvertirRequestDTO;
import com.alkywallet.service.InversionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inversiones")
@RequiredArgsConstructor
public class InversionController {

    private final InversionService inversionService;

    @GetMapping
    public ResponseEntity<List<InversionDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(inversionService.obtenerPorEmail(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<InversionDTO> invertir(
            @Valid @RequestBody InvertirRequestDTO request,
            Authentication authentication
    ) {
        InversionDTO creada = inversionService.invertir(authentication.getName(), request.monto());
        return ResponseEntity.status(201).body(creada);
    }

    @PostMapping("/{id}/rescatar")
    public ResponseEntity<InversionDTO> rescatar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(inversionService.rescatar(authentication.getName(), id));
    }
}
