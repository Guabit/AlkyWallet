package com.alkywallet.controller;

import com.alkywallet.dto.PreguntaAsistenteDTO;
import com.alkywallet.dto.RespuestaAsistenteDTO;
import com.alkywallet.service.AsistenteIAService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asistente")
@RequiredArgsConstructor
public class AsistenteIAController {

    private final AsistenteIAService asistenteIAService;

    @PostMapping("/preguntar")
    public ResponseEntity<RespuestaAsistenteDTO> preguntar(
            @Valid @RequestBody PreguntaAsistenteDTO request,
            Authentication authentication
    ) {
        String respuesta = asistenteIAService.responder(authentication.getName(), request.pregunta());
        return ResponseEntity.ok(new RespuestaAsistenteDTO(respuesta));
    }
}
