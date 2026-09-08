package com.alkywallet.controller;

import com.alkywallet.dto.PagoServicioRequestDTO;
import com.alkywallet.dto.PagoServicioResponseDTO;
import com.alkywallet.service.PedidosYaMockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidosya")
@RequiredArgsConstructor
public class PedidosYaController {

    private final PedidosYaMockService pedidosYaMockService;

    @PostMapping("/pagar")
    public ResponseEntity<PagoServicioResponseDTO> pagar(
            @Valid @RequestBody PagoServicioRequestDTO request,
            Authentication authentication
    ) {
        PagoServicioResponseDTO resultado = pedidosYaMockService.pagarPedido(
                authentication.getName(), request.comercio(), request.monto());
        return ResponseEntity.ok(resultado);
    }
}
