package com.alkywallet.controller;

import com.alkywallet.dto.RegisterRequest;
import com.alkywallet.dto.UserRequestDTO;
import com.alkywallet.dto.UserResponseDTO;
import com.alkywallet.dto.UserUpdateDTO;
import com.alkywallet.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> crear(@Valid @RequestBody UserRequestDTO request) {
        UserResponseDTO creado = userService.crear(request);
        return ResponseEntity
                .created(URI.create("/api/usuarios/" + creado.id()))
                .body(creado);
    }

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, String>> registrarUsuario(@Valid @RequestBody RegisterRequest request) {
        userService.registrarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Usuario registrado exitosamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> obtenerPerfil(Authentication authentication) {
        return ResponseEntity.ok(userService.obtenerPorEmail(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> actualizarPerfil(
            Authentication authentication,
            @Valid @RequestBody UserUpdateDTO request
    ) {
        return ResponseEntity.ok(userService.actualizarPorEmail(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(userService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(userService.obtenerTodos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO request
    ) {
        return ResponseEntity.ok(userService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        userService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}