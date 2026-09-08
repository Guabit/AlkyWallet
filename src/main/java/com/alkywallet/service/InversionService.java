package com.alkywallet.service;

import com.alkywallet.dto.InversionDTO;
import com.alkywallet.entity.CategoriaTransaccion;
import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.Inversion;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.entity.TipoTransaccion;
import com.alkywallet.entity.Transaccion;
import com.alkywallet.entity.Usuario;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.InversionRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Simula una colocación en un fondo común de inversión (FCI) tipo
 * "money market" dentro de la propia billetera: al invertir se descuenta
 * el saldo de la Cuenta ARS del usuario, y al rescatar se devuelve el
 * capital más un rendimiento calculado con devengamiento diario simple
 * (montoInvertido * tasaAnualNominal / 365 * díasTranscurridos).
 *
 * Es una simulación con fines demostrativos: no representa una inversión
 * real ni una tasa de mercado vigente.
 */
@Service
@RequiredArgsConstructor
public class InversionService {

    private static final int DIAS_ANIO = 365;

    private final InversionRepository inversionRepository;
    private final CuentaRepository cuentaRepository;
    private final UserRepository userRepository;
    private final TransaccionRepository transaccionRepository;

    @Transactional
    public InversionDTO invertir(String email, Double monto) {
        if (monto == null || monto <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto a invertir debe ser mayor a cero");
        }

        Cuenta cuenta = obtenerCuentaArsPorEmail(email);
        BigDecimal montoBigDecimal = BigDecimal.valueOf(monto);

        if (cuenta.getSaldo().compareTo(montoBigDecimal) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente para realizar esta inversión");
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(montoBigDecimal));
        cuentaRepository.save(cuenta);

        Inversion inversion = Inversion.builder()
                .cuenta(cuenta)
                .montoInvertido(montoBigDecimal)
                .activa(true)
                .build();
        inversion = inversionRepository.save(inversion);

        registrarMovimiento(cuenta, TipoTransaccion.EGRESO, montoBigDecimal,
                "Inversión en FCI AlkyWallet (simulado)", CategoriaTransaccion.INVERSION);

        return toDTO(inversion);
    }

    @Transactional
    public InversionDTO rescatar(String email, Long inversionId) {
        Cuenta cuenta = obtenerCuentaArsPorEmail(email);

        Inversion inversion = inversionRepository.findByIdAndCuentaId(inversionId, cuenta.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inversión no encontrada"));

        if (!inversion.isActiva()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta inversión ya fue rescatada");
        }

        BigDecimal rendimiento = calcularRendimiento(inversion, LocalDateTime.now());
        BigDecimal valorFinal = inversion.getMontoInvertido().add(rendimiento);

        cuenta.setSaldo(cuenta.getSaldo().add(valorFinal));
        cuentaRepository.save(cuenta);

        inversion.setActiva(false);
        inversion.setFechaRescate(LocalDateTime.now());
        inversion = inversionRepository.save(inversion);

        registrarMovimiento(cuenta, TipoTransaccion.INGRESO, valorFinal,
                "Rescate de inversión FCI + rendimiento simulado", CategoriaTransaccion.INVERSION);

        return toDTO(inversion);
    }

    @Transactional(readOnly = true)
    public List<InversionDTO> obtenerPorEmail(String email) {
        Cuenta cuenta = obtenerCuentaArsPorEmail(email);
        return inversionRepository.findByCuentaIdOrderByFechaInicioDesc(cuenta.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private Cuenta obtenerCuentaArsPorEmail(String email) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), TipoMoneda.ARS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    }

    private void registrarMovimiento(Cuenta cuenta, TipoTransaccion tipo, BigDecimal monto,
                                      String concepto, CategoriaTransaccion categoria) {
        Transaccion transaccion = Transaccion.builder()
                .monto(monto)
                .fecha(LocalDateTime.now())
                .tipo(tipo)
                .concepto(concepto)
                .categoria(categoria)
                .cuenta(cuenta)
                .build();
        transaccionRepository.save(transaccion);
    }

    /**
     * Devengamiento diario simple: montoInvertido * tasaAnualNominal / 365 * díasTranscurridos.
     */
    private BigDecimal calcularRendimiento(Inversion inversion, LocalDateTime hasta) {
        long dias = Math.max(0, ChronoUnit.DAYS.between(inversion.getFechaInicio(), hasta));
        return inversion.getMontoInvertido()
                .multiply(inversion.getTasaAnualNominal())
                .multiply(BigDecimal.valueOf(dias))
                .divide(BigDecimal.valueOf(DIAS_ANIO), 2, RoundingMode.HALF_UP);
    }

    private InversionDTO toDTO(Inversion inversion) {
        LocalDateTime hasta = inversion.isActiva() ? LocalDateTime.now() : inversion.getFechaRescate();
        long dias = Math.max(0, ChronoUnit.DAYS.between(inversion.getFechaInicio(), hasta));
        BigDecimal rendimiento = calcularRendimiento(inversion, hasta);

        return InversionDTO.builder()
                .id(inversion.getId())
                .montoInvertido(inversion.getMontoInvertido())
                .tasaAnualNominal(inversion.getTasaAnualNominal())
                .fechaInicio(inversion.getFechaInicio())
                .fechaRescate(inversion.getFechaRescate())
                .diasTranscurridos(dias)
                .rendimientoSimulado(rendimiento)
                .valorActual(inversion.getMontoInvertido().add(rendimiento))
                .activa(inversion.isActiva())
                .build();
    }
}
