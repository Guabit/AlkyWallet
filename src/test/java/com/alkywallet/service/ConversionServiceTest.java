package com.alkywallet.service;

import com.alkywallet.entity.*;
import com.alkywallet.exception.MonedaIncompatibleException;
import com.alkywallet.exception.SaldoInsuficienteException;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.TransaccionRepository;
import com.alkywallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @InjectMocks
    private ConversionService conversionService;

    private Usuario usuario;
    private Cuenta cuentaArs;
    private Cuenta cuentaUsd;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).email("test@test.com").build();

        cuentaArs = Cuenta.builder()
                .id(100L)
                .usuario(usuario)
                .saldo(new BigDecimal("10000.00"))
                .tipoMoneda(TipoMoneda.ARS)
                .build();

        cuentaUsd = Cuenta.builder()
                .id(200L)
                .usuario(usuario)
                .saldo(new BigDecimal("50.00"))
                .tipoMoneda(TipoMoneda.USD)
                .build();
    }

    @Test
    void convertirDivisas_ARS_a_USD_Exito() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuarioIdAndTipoMoneda(1L, TipoMoneda.ARS)).thenReturn(Optional.of(cuentaArs));
        when(cuentaRepository.findByUsuarioIdAndTipoMoneda(1L, TipoMoneda.USD)).thenReturn(Optional.of(cuentaUsd));
        
        when(cuentaRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(cuentaArs));
        when(cuentaRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(cuentaUsd));
        
        // 1 ARS = 0.001 USD (for example, rate 1000 ARS/USD -> 0.001)
        when(exchangeRateProvider.getExchangeRate(TipoMoneda.ARS, TipoMoneda.USD)).thenReturn(new BigDecimal("0.001"));

        conversionService.convertirDivisas("test@test.com", TipoMoneda.ARS, TipoMoneda.USD, new BigDecimal("5000.00"));

        assertEquals(new BigDecimal("5000.00"), cuentaArs.getSaldo()); // 10000 - 5000
        assertEquals(new BigDecimal("55.00"), cuentaUsd.getSaldo());   // 50 + (5000 * 0.001 = 5)

        verify(cuentaRepository, times(2)).save(any(Cuenta.class));
        verify(transaccionRepository, times(2)).save(any(Transaccion.class));
    }

    @Test
    void convertirDivisas_MismaMoneda_LanzaException() {
        assertThrows(MonedaIncompatibleException.class, () -> 
            conversionService.convertirDivisas("test@test.com", TipoMoneda.ARS, TipoMoneda.ARS, new BigDecimal("100.00"))
        );
    }
    
    @Test
    void convertirDivisas_SaldoInsuficiente_LanzaException() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuarioIdAndTipoMoneda(1L, TipoMoneda.ARS)).thenReturn(Optional.of(cuentaArs));
        when(cuentaRepository.findByUsuarioIdAndTipoMoneda(1L, TipoMoneda.USD)).thenReturn(Optional.of(cuentaUsd));
        
        when(cuentaRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(cuentaArs));
        when(cuentaRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(cuentaUsd));
        
        assertThrows(SaldoInsuficienteException.class, () -> 
            conversionService.convertirDivisas("test@test.com", TipoMoneda.ARS, TipoMoneda.USD, new BigDecimal("50000.00"))
        );
    }
}
