package com.alkywallet.service;

import com.alkywallet.dto.InversionDTO;
import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.Inversion;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.entity.Transaccion;
import com.alkywallet.entity.Usuario;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.InversionRepository;
import com.alkywallet.repository.TransaccionRepository;
import com.alkywallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InversionServiceTest {

    @Mock
    private InversionRepository inversionRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransaccionRepository transaccionRepository;

    @InjectMocks
    private InversionService inversionService;

    private Usuario usuario;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).email("ana@mail.com").build();
        cuenta = Cuenta.builder()
                .id(10L)
                .usuario(usuario)
                .saldo(new BigDecimal("1000.00"))
                .tipoMoneda(TipoMoneda.ARS)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Happy Path: invertir descuenta el saldo y crea la inversión activa")
    void invertir_HappyPath_DebeDescontarSaldoYCrearInversion() {
        when(userRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuarioIdAndTipoMoneda(1L, TipoMoneda.ARS)).thenReturn(Optional.of(cuenta));
        when(inversionRepository.save(any(Inversion.class))).thenAnswer(inv -> {
            Inversion i = inv.getArgument(0);
            i.setId(99L);
            if (i.getFechaInicio() == null) {
                // Simula lo que @CreationTimestamp haría al persistir con Hibernate real
                i.setFechaInicio(LocalDateTime.now());
            }
            return i;
        });

        InversionDTO resultado = inversionService.invertir("ana@mail.com", 200.0);

        assertEquals(new BigDecimal("800.00"), cuenta.getSaldo());
        assertTrue(resultado.isActiva());
        assertEquals(0, new BigDecimal("200.0").compareTo(resultado.getMontoInvertido()));

        verify(cuentaRepository).save(cuenta);
        verify(inversionRepository).save(any(Inversion.class));
        verify(transaccionRepository).save(any(Transaccion.class));
    }

    @Test
    @DisplayName("Edge Case: invertir más del saldo disponible lanza excepción y no persiste nada")
    void invertir_SaldoInsuficiente_DebeLanzarExcepcion() {
        when(userRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuarioIdAndTipoMoneda(1L, TipoMoneda.ARS)).thenReturn(Optional.of(cuenta));

        assertThrows(ResponseStatusException.class, () -> inversionService.invertir("ana@mail.com", 5000.0));

        assertEquals(new BigDecimal("1000.00"), cuenta.getSaldo());
        verify(inversionRepository, never()).save(any(Inversion.class));
        verify(transaccionRepository, never()).save(any(Transaccion.class));
    }

    @Test
    @DisplayName("Rescatar: calcula el rendimiento simulado con devengamiento diario y acredita capital + rendimiento")
    void rescatar_DebeCalcularRendimientoYAcreditarCapitalMasRendimiento() {
        Inversion inversionActiva = Inversion.builder()
                .id(5L)
                .cuenta(cuenta)
                .montoInvertido(new BigDecimal("1000.00"))
                .tasaAnualNominal(new BigDecimal("0.365")) // tasa elegida para que el cálculo dé un número redondo
                .activa(true)
                .fechaInicio(LocalDateTime.now().minusDays(10))
                .build();

        when(userRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuarioIdAndTipoMoneda(1L, TipoMoneda.ARS)).thenReturn(Optional.of(cuenta));
        when(inversionRepository.findByIdAndCuentaId(5L, 10L)).thenReturn(Optional.of(inversionActiva));
        when(inversionRepository.save(any(Inversion.class))).thenAnswer(inv -> inv.getArgument(0));

        InversionDTO resultado = inversionService.rescatar("ana@mail.com", 5L);

        // 1000.00 * 0.365 / 365 * 10 días = 10.00 de rendimiento
        assertEquals(new BigDecimal("10.00"), resultado.getRendimientoSimulado());
        assertEquals(new BigDecimal("1010.00"), resultado.getValorActual());
        assertFalse(resultado.isActiva());
        // El saldo de la cuenta (1000.00 inicial, ajeno a esta inversión) recibe capital + rendimiento
        assertEquals(new BigDecimal("2010.00"), cuenta.getSaldo());

        verify(cuentaRepository).save(cuenta);
        verify(transaccionRepository).save(any(Transaccion.class));
    }
}
