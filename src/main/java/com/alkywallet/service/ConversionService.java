package com.alkywallet.service;

import com.alkywallet.exception.CuentaNoPertenecienteException;
import com.alkywallet.exception.MonedaIncompatibleException;
import com.alkywallet.exception.ResourceNotFoundException;
import com.alkywallet.exception.SaldoInsuficienteException;
import com.alkywallet.entity.CategoriaTransaccion;
import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.entity.TipoTransaccion;
import com.alkywallet.entity.Transaccion;
import com.alkywallet.entity.Usuario;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.TransaccionRepository;
import com.alkywallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConversionService {
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final UserRepository userRepository;
    private final ExchangeRateProvider exchangeRateProvider;

    @Transactional
    public void convertirDivisas(String email, TipoMoneda monedaOrigen, TipoMoneda monedaDestino, BigDecimal montoOrigen) {
        if (monedaOrigen == monedaDestino) {
            throw new MonedaIncompatibleException("Las monedas origen y destino deben ser diferentes");
        }

        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuentaOrigenSearch = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), monedaOrigen)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta origen (" + monedaOrigen + ") no encontrada"));
                
        Cuenta cuentaDestinoSearch = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), monedaDestino)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta destino (" + monedaDestino + ") no encontrada"));

        // PREVENT DEADLOCKS: Order acquiring pessimistic locks by ID
        Long minId = Math.min(cuentaOrigenSearch.getId(), cuentaDestinoSearch.getId());
        Long maxId = Math.max(cuentaOrigenSearch.getId(), cuentaDestinoSearch.getId());
        
        Cuenta firstLock = cuentaRepository.findByIdForUpdate(minId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta " + minId + " no encontrada"));
        Cuenta secondLock = cuentaRepository.findByIdForUpdate(maxId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta " + maxId + " no encontrada"));

        Cuenta cuentaOrigen = cuentaOrigenSearch.getId().equals(minId) ? firstLock : secondLock;
        Cuenta cuentaDestino = cuentaDestinoSearch.getId().equals(minId) ? firstLock : secondLock;

        if (!cuentaOrigen.getUsuario().getId().equals(usuario.getId()) || !cuentaDestino.getUsuario().getId().equals(usuario.getId())) {
             throw new CuentaNoPertenecienteException("Ambas cuentas deben pertenecer al usuario autenticado");
        }

        if (cuentaOrigen.getSaldo().compareTo(montoOrigen) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta " + monedaOrigen);
        }

        BigDecimal rate = exchangeRateProvider.getExchangeRate(monedaOrigen, monedaDestino);
        BigDecimal montoDestino = montoOrigen.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);

        // --- Débito ---
        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(montoOrigen));
        cuentaRepository.save(cuentaOrigen);

        Transaccion egreso = Transaccion.builder()
                .monto(montoOrigen)
                .fecha(LocalDateTime.now())
                .tipo(TipoTransaccion.EGRESO)
                .concepto("Conversión de " + monedaOrigen + " a " + monedaDestino)
                .categoria(CategoriaTransaccion.OTROS)
                .cuenta(cuentaOrigen)
                .build();
        transaccionRepository.save(egreso);

        // --- Crédito ---
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(montoDestino));
        cuentaRepository.save(cuentaDestino);

        Transaccion ingreso = Transaccion.builder()
                .monto(montoDestino)
                .fecha(LocalDateTime.now())
                .tipo(TipoTransaccion.INGRESO)
                .concepto("Conversión desde " + monedaOrigen)
                .categoria(CategoriaTransaccion.OTROS)
                .cuenta(cuentaDestino)
                .build();
        transaccionRepository.save(ingreso);
    }
}
