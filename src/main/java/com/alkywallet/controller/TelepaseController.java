package com.alkywallet.controller;

import com.alkywallet.dto.PagoPeajeRequestDTO;
import com.alkywallet.dto.PagoServicioResponseDTO;
import com.alkywallet.service.TelepaseMockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telepase")
@RequiredArgsConstructor
public class TelepaseController {

    private final TelepaseMockService telepaseMockService;

    @PostMapping("/pagar")
    public ResponseEntity<PagoServicioResponseDTO> pagar(
            @Valid @RequestBody PagoPeajeRequestDTO request,
            Authentication authentication
    ) {
        PagoServicioResponseDTO resultado = telepaseMockService.pagarPeaje(
                authentication.getName(), request.monto(), request.patente());
        return ResponseEntity.ok(resultado);
    }
}
