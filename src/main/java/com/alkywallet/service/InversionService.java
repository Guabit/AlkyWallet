package com.alkywallet.service;

import com.alkywallet.dto.InversionDTO;
import com.alkywallet.exception.ResourceNotFoundException;
import com.alkywallet.exception.SaldoInsuficienteException;
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

@Service
@RequiredArgsConstructor
public class InversionService {

    private static final int DIAS_ANIO = 365;
    private static final BigDecimal TASA_ARS = new BigDecimal("0.40"); // 40% TNA
    private static final BigDecimal TASA_USD = new BigDecimal("0.04"); // 4% TNA

    private final InversionRepository inversionRepository;
    private final CuentaRepository cuentaRepository;
    private final UserRepository userRepository;
    private final TransaccionRepository transaccionRepository;

    @Transactional
    public InversionDTO invertir(String email, Double monto, TipoMoneda tipoMoneda) {
        if (monto == null || monto <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto a invertir debe ser mayor a cero");
        }
        
        TipoMoneda moneda = (tipoMoneda != null) ? tipoMoneda : TipoMoneda.ARS;
        Cuenta cuenta = obtenerCuentaPorEmailYMoneda(email, moneda);
        BigDecimal montoBigDecimal = BigDecimal.valueOf(monto);

        if (cuenta.getSaldo().compareTo(montoBigDecimal) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente en " + moneda + " para realizar esta inversión");
        }
        
        // Locking to prevent concurrent transfers depleting the account
        cuenta = cuentaRepository.findByIdForUpdate(cuenta.getId())
                 .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada"));
                 
        if (cuenta.getSaldo().compareTo(montoBigDecimal) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente en " + moneda + " para realizar esta inversión");
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(montoBigDecimal));
        cuentaRepository.save(cuenta);

        BigDecimal tasaSimulada = (moneda == TipoMoneda.USD) ? TASA_USD : TASA_ARS;

        Inversion inversion = Inversion.builder()
                .cuenta(cuenta)
                .montoInvertido(montoBigDecimal)
                .tasaAnualNominal(tasaSimulada)
                .activa(true)
                .build();
        inversion = inversionRepository.save(inversion);

        registrarMovimiento(cuenta, TipoTransaccion.EGRESO, montoBigDecimal,
                "Inversión en FCI AlkyWallet (simulado)");

        return toDTO(inversion);
    }

    @Transactional
    public InversionDTO rescatar(String email, Long inversionId) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
                
        // Here we first search the inversion, no matter the currency
        Inversion inversion = inversionRepository.findById(inversionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inversión no encontrada"));
                
        if (!inversion.getCuenta().getUsuario().getId().equals(usuario.getId())) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La inversión no te pertenece");
        }

        if (!inversion.isActiva()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta inversión ya fue rescatada");
        }
        
        Cuenta cuenta = cuentaRepository.findByIdForUpdate(inversion.getCuenta().getId())
                 .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada"));

        BigDecimal rendimiento = calcularRendimiento(inversion, LocalDateTime.now());
        BigDecimal valorFinal = inversion.getMontoInvertido().add(rendimiento).setScale(2, RoundingMode.HALF_EVEN);

        cuenta.setSaldo(cuenta.getSaldo().add(valorFinal));
        cuentaRepository.save(cuenta);

        inversion.setActiva(false);
        inversion.setFechaRescate(LocalDateTime.now());
        inversion = inversionRepository.save(inversion);

        registrarMovimiento(cuenta, TipoTransaccion.INGRESO, valorFinal,
                "Rescate de inversión FCI + rendimiento simulado");

        return toDTO(inversion);
    }

    @Transactional(readOnly = true)
    public List<InversionDTO> obtenerPorEmail(String email, TipoMoneda moneda) {
        if (moneda != null) {
            Cuenta cuenta = obtenerCuentaPorEmailYMoneda(email, moneda);
            return inversionRepository.findByCuentaIdOrderByFechaInicioDesc(cuenta.getId())
                    .stream().map(this::toDTO).toList();
        } else {
             // If missing, return all user's accounts investments
             Usuario usuario = userRepository.findByEmail(email).orElseThrow();
             List<Cuenta> cuentas = cuentaRepository.findByUsuarioId(usuario.getId());
             return cuentas.stream()
                .flatMap(c -> inversionRepository.findByCuentaIdOrderByFechaInicioDesc(c.getId()).stream())
                .map(this::toDTO)
                .sorted((a, b) -> b.getFechaInicio().compareTo(a.getFechaInicio()))
                .toList();
        }
    }

    private Cuenta obtenerCuentaPorEmailYMoneda(String email, TipoMoneda moneda) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada en " + moneda));
    }

    private void registrarMovimiento(Cuenta cuenta, TipoTransaccion tipo, BigDecimal monto,
                                      String concepto) {
        Transaccion transaccion = Transaccion.builder()
                .monto(monto)
                .fecha(LocalDateTime.now())
                .tipo(tipo)
                .concepto(concepto)
                .categoria(CategoriaTransaccion.INVERSION)
                .cuenta(cuenta)
                .build();
        transaccionRepository.save(transaccion);
    }

    private BigDecimal calcularRendimiento(Inversion inversion, LocalDateTime hasta) {
        long dias = Math.max(0, ChronoUnit.DAYS.between(inversion.getFechaInicio(), hasta));
        return inversion.getMontoInvertido()
                .multiply(inversion.getTasaAnualNominal())
                .multiply(BigDecimal.valueOf(dias))
                .divide(BigDecimal.valueOf(DIAS_ANIO), 2, RoundingMode.HALF_EVEN);
    }

    private InversionDTO toDTO(Inversion inversion) {
        LocalDateTime hasta = inversion.isActiva() ? LocalDateTime.now() : inversion.getFechaRescate();
        long dias = Math.max(0, ChronoUnit.DAYS.between(inversion.getFechaInicio(), hasta));
        BigDecimal rendimiento = calcularRendimiento(inversion, hasta);

        return InversionDTO.builder()
                .id(inversion.getId())
                .moneda(inversion.getCuenta().getTipoMoneda())
                .montoInvertido(inversion.getMontoInvertido())
                .tasaAnualNominal(inversion.getTasaAnualNominal())
                .fechaInicio(inversion.getFechaInicio())
                .fechaRescate(inversion.getFechaRescate())
                .diasTranscurridos(dias)
                .rendimientoSimulado(rendimiento)
                .valorActual(inversion.getMontoInvertido().add(rendimiento).setScale(2, RoundingMode.HALF_EVEN))
                .activa(inversion.isActiva())
                .build();
    }
}
