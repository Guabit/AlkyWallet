package com.alkywallet.service;

import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.entity.TipoTransaccion;
import com.alkywallet.entity.Transaccion;
import com.alkywallet.exception.SaldoInsuficienteException;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.TransaccionRepository;
import com.alkywallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransaccionServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransaccionService transaccionService;

    private Cuenta cuentaA;
    private Cuenta cuentaB;

    @BeforeEach
    void setUp() {
        cuentaA = Cuenta.builder()
                .id(1L)
                .saldo(new BigDecimal("1000.00"))
                .tipoMoneda(TipoMoneda.ARS)
                .isDeleted(false)
                .build();

        cuentaB = Cuenta.builder()
                .id(2L)
                .saldo(new BigDecimal("500.00"))
                .tipoMoneda(TipoMoneda.ARS)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Happy Path: Transferencia exitosa entre Cuenta A y Cuenta B")
    void realizarTransferencia_HappyPath_DebeActualizarSaldosYGuardarTransacciones() {
        // GIVEN: Escenario donde Cuenta A ($1000) transfiere $200 a Cuenta B ($500)
        Double montoATransferir = 200.00;
        BigDecimal montoBigDecimal = BigDecimal.valueOf(montoATransferir);

        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaA));
        when(cuentaRepository.findById(2L)).thenReturn(Optional.of(cuentaB));

        // WHEN: Se ejecuta la transferencia
        transaccionService.realizarTransferencia(1L, 2L, montoATransferir);

        // THEN:
        // 1. El saldo de A disminuyó exactamente en el monto enviado ($800.00)
        assertEquals(new BigDecimal("800.00"), cuentaA.getSaldo());

        // 2. El saldo de B aumentó exactamente en esa misma proporción ($700.00)
        assertEquals(new BigDecimal("700.00"), cuentaB.getSaldo());

        // 3. Verificamos con Mockito que ambas cuentas fueron guardadas
        verify(cuentaRepository).save(cuentaA);
        verify(cuentaRepository).save(cuentaB);

        // 4. Verificamos que se registraron los dos movimientos contables (egreso e ingreso)
        verify(transaccionRepository, times(2)).save(any(Transaccion.class));
    }

    @Test
    @DisplayName("Edge Case: Saldo insuficiente debe lanzar excepción y no guardar nada")
    void realizarTransferencia_SaldoInsuficiente_DebeLanzarExcepcionYNoGuardar() {
        // GIVEN: Cuenta A solo tiene $1000 e intenta transferir $1500
        Double montoExcesivo = 1500.00;

        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaA));
        when(cuentaRepository.findById(2L)).thenReturn(Optional.of(cuentaB));

        // WHEN & THEN:
        // 1. Verificar que se lanza correctamente SaldoInsuficienteException
        SaldoInsuficienteException excepcion = assertThrows(
                SaldoInsuficienteException.class,
                () -> transaccionService.realizarTransferencia(1L, 2L, montoExcesivo)
        );

        assertEquals("Saldo insuficiente en la cuenta origen", excepcion.getMessage());

        // 2. Verificar que los saldos en memoria NO sufrieron alteraciones
        assertEquals(new BigDecimal("1000.00"), cuentaA.getSaldo());
        assertEquals(new BigDecimal("500.00"), cuentaB.getSaldo());

        // 3. Verificar con Mockito que NUNCA se llamaron los métodos save()
        verify(cuentaRepository, never()).save(any(Cuenta.class));
        verify(transaccionRepository, never()).save(any(Transaccion.class));
    }
}