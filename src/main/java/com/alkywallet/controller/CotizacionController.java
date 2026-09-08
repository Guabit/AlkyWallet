package com.alkywallet.controller;

import com.alkywallet.dto.CotizacionDolarDTO;
import com.alkywallet.service.CotizacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cotizacion")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @GetMapping("/dolar")
    public ResponseEntity<List<CotizacionDolarDTO>> obtenerCotizacionDolar() {
        return ResponseEntity.ok(cotizacionService.obtenerCotizaciones());
    }
}
