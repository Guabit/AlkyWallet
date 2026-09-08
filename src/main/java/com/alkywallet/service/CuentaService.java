package com.alkywallet.service;

import com.alkywallet.dto.CuentaDTO;
import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.entity.Usuario;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CuentaService {
    private final CuentaRepository cuentaRepository;
    private final UserRepository userRepository;

    @Transactional
    public CuentaDTO obtenerBalancePorUsuarioId(Long usuarioId) {
        return obtenerBalancePorUsuarioId(usuarioId, TipoMoneda.ARS);
    }

    @Transactional
    public CuentaDTO obtenerBalancePorUsuarioId(Long usuarioId, TipoMoneda moneda) {
        Usuario usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda)
                .orElseGet(() -> crearCuentaInicial(usuario, moneda));

        return CuentaDTO.builder()
                .id(cuenta.getId())
                .saldo(cuenta.getSaldo())
                .tipoMoneda(cuenta.getTipoMoneda())
                .build();
    }

    @Transactional
    public CuentaDTO obtenerBalancePorEmail(String email) {
        return obtenerBalancePorEmail(email, TipoMoneda.ARS);
    }

    @Transactional
    public CuentaDTO obtenerBalancePorEmail(String email, TipoMoneda moneda) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda)
                .orElseGet(() -> crearCuentaInicial(usuario, moneda));

        return CuentaDTO.builder()
                .id(cuenta.getId())
                .saldo(cuenta.getSaldo())
                .tipoMoneda(cuenta.getTipoMoneda())
                .build();
    }

    @Transactional
    public CuentaDTO crearCuenta(String email, TipoMoneda moneda) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya posee una cuenta en " + moneda);
        }

        Cuenta cuenta = crearCuentaInicial(usuario, moneda);
        return CuentaDTO.builder()
                .id(cuenta.getId())
                .saldo(cuenta.getSaldo())
                .tipoMoneda(cuenta.getTipoMoneda())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CuentaDTO> obtenerCuentasPorEmail(String email) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return cuentaRepository.findByUsuarioId(usuario.getId()).stream()
                .map(c -> CuentaDTO.builder()
                        .id(c.getId())
                        .saldo(c.getSaldo())
                        .tipoMoneda(c.getTipoMoneda())
                        .build())
                .toList();
    }

    private Cuenta crearCuentaInicial(Usuario usuario, TipoMoneda moneda) {
        Cuenta nuevaCuenta = Cuenta.builder()
                .usuario(usuario)
                .saldo(BigDecimal.ZERO)
                .tipoMoneda(moneda)
                .isDeleted(false)
                .build();

        return cuentaRepository.save(nuevaCuenta);
    }
}
