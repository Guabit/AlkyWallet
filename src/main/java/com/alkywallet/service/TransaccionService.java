package com.alkywallet.service;

import com.alkywallet.dto.GastoPorCategoriaDTO;
import com.alkywallet.dto.GastoPorTipoDTO;
import com.alkywallet.dto.TransaccionDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransaccionService {
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void realizarDepositoPorEmail(String email, Double monto) {
        realizarDepositoPorEmail(email, monto, TipoMoneda.ARS);
    }
    
    @Transactional
    public void realizarDepositoPorEmail(String email, Double monto, TipoMoneda moneda) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda)
                .orElseGet(() -> {
                    Cuenta nueva = Cuenta.builder()
                            .usuario(usuario)
                            .saldo(BigDecimal.ZERO)
                            .tipoMoneda(moneda)
                            .isDeleted(false)
                            .build();
                    return cuentaRepository.save(nueva);
                });

        realizarDeposito(cuenta.getId(), monto);
    }

    @Transactional
    public void realizarDeposito(Long cuentaId, Double monto) {
        if (cuentaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de la cuenta es obligatorio");
        }
        if (monto == null || monto <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
        }

        Cuenta cuenta = cuentaRepository.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        if (cuenta.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cuenta destino inactiva");
        }

        BigDecimal montoBigDecimal = BigDecimal.valueOf(monto);
        BigDecimal nuevoSaldo = cuenta.getSaldo().add(montoBigDecimal);
        cuenta.setSaldo(nuevoSaldo);

        cuentaRepository.save(cuenta);

        Transaccion transaccion = Transaccion.builder()
                .monto(montoBigDecimal)
                .fecha(LocalDateTime.now())
                .tipo(TipoTransaccion.DEPOSITO)
                .concepto("Deposito en cuenta")
                .cuenta(cuenta)
                .build();

        transaccionRepository.save(transaccion);
    }

    @Transactional
    public void realizarTransferenciaPorEmail(String emailOrigen, String destinatarioEmail, Double monto) {
        realizarTransferenciaPorEmail(emailOrigen, destinatarioEmail, monto, CategoriaTransaccion.TRANSFERENCIA, TipoMoneda.ARS);
    }

    @Transactional
    public void realizarTransferenciaPorEmail(String emailOrigen, String destinatarioEmail, Double monto,
                                               CategoriaTransaccion categoria) {
        realizarTransferenciaPorEmail(emailOrigen, destinatarioEmail, monto, categoria, TipoMoneda.ARS);
    }
    
    @Transactional
    public void realizarTransferenciaPorEmail(String emailOrigen, String destinatarioEmail, Double monto,
                                               CategoriaTransaccion categoria, TipoMoneda moneda) {
        if (emailOrigen.trim().equalsIgnoreCase(destinatarioEmail.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No podés transferirte dinero a vos mismo");
        }

        Usuario usuarioOrigen = userRepository.findByEmail(emailOrigen)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario origen no encontrado"));

        Cuenta cuentaOrigen = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuarioOrigen.getId(), moneda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta origen no encontrada"));

        Usuario usuarioDestino = userRepository.findByEmail(destinatarioEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un usuario con el email: " + destinatarioEmail));

        Cuenta cuentaDestino = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuarioDestino.getId(), moneda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El destinatario no posee una cuenta activa en " + moneda));

        realizarTransferencia(cuentaOrigen.getId(), cuentaDestino.getId(), monto, categoria);
    }

    @Transactional
    public void realizarTransferencia(Long cuentaOrigenId, Long cuentaDestinoId, Double monto) {
        realizarTransferencia(cuentaOrigenId, cuentaDestinoId, monto, CategoriaTransaccion.TRANSFERENCIA);
    }

    @Transactional
    public void realizarTransferencia(Long cuentaOrigenId, Long cuentaDestinoId, Double monto,
                                       CategoriaTransaccion categoriaSolicitada) {
        if (cuentaOrigenId == null || cuentaDestinoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las cuentas de origen y destino son obligatorias");
        }
        if (cuentaOrigenId.equals(cuentaDestinoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta destino debe ser distinta a la cuenta origen");
        }
        if (monto == null || monto <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
        }

        CategoriaTransaccion categoria = categoriaSolicitada != null ? categoriaSolicitada : CategoriaTransaccion.TRANSFERENCIA;

        // PREVENT DEADLOCKS: Order acquiring pessimistic locks by ID
        Long minId = Math.min(cuentaOrigenId, cuentaDestinoId);
        Long maxId = Math.max(cuentaOrigenId, cuentaDestinoId);
        
        Cuenta firstLock = cuentaRepository.findByIdForUpdate(minId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta " + minId + " no encontrada"));
        Cuenta secondLock = cuentaRepository.findByIdForUpdate(maxId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta " + maxId + " no encontrada"));

        Cuenta cuentaOrigen = cuentaOrigenId.equals(minId) ? firstLock : secondLock;
        Cuenta cuentaDestino = cuentaDestinoId.equals(minId) ? firstLock : secondLock;

        if (cuentaOrigen.isDeleted() || cuentaDestino.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede transferir desde o hacia una cuenta inactiva");
        }
        
        if (cuentaOrigen.getTipoMoneda() != cuentaDestino.getTipoMoneda()) {
            throw new MonedaIncompatibleException("No se pueden realizar transferencias directas entre diferentes monedas");
        }

        BigDecimal montoBigDecimal = BigDecimal.valueOf(monto);

        if (cuentaOrigen.getSaldo().compareTo(montoBigDecimal) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta origen");
        }

        String infoDestino = (cuentaDestino.getUsuario() != null && cuentaDestino.getUsuario().getEmail() != null)
                ? cuentaDestino.getUsuario().getEmail()
                : String.valueOf(cuentaDestinoId);

        String infoOrigen = (cuentaOrigen.getUsuario() != null && cuentaOrigen.getUsuario().getEmail() != null)
                ? cuentaOrigen.getUsuario().getEmail()
                : String.valueOf(cuentaOrigenId);

        // --- Débito en cuenta origen ---
        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(montoBigDecimal));
        cuentaRepository.save(cuentaOrigen);

        Transaccion egreso = Transaccion.builder()
                .monto(montoBigDecimal)
                .fecha(LocalDateTime.now())
                .tipo(TipoTransaccion.EGRESO)
                .concepto("Transferencia a cuenta " + infoDestino)
                .categoria(categoria)
                .cuenta(cuentaOrigen)
                .build();
        transaccionRepository.save(egreso);

        // --- Crédito en cuenta destino ---
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(montoBigDecimal));
        cuentaRepository.save(cuentaDestino);

        Transaccion ingreso = Transaccion.builder()
                .monto(montoBigDecimal)
                .fecha(LocalDateTime.now())
                .tipo(TipoTransaccion.INGRESO)
                .concepto("Transferencia desde cuenta " + infoOrigen)
                .categoria(categoria)
                .cuenta(cuentaDestino)
                .build();
        transaccionRepository.save(ingreso);
    }

    @Transactional(readOnly = true)
    public List<TransaccionDTO> obtenerHistorialPorEmail(String email) {
        return obtenerHistorialPorEmail(email, TipoMoneda.ARS);
    }

    @Transactional(readOnly = true)
    public List<TransaccionDTO> obtenerHistorialPorEmail(String email, TipoMoneda moneda) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        return transaccionRepository.obtenerHistorialPorCuentaId(cuenta.getId());
    }

    @Transactional(readOnly = true)
    public List<GastoPorTipoDTO> obtenerReporteGastosPorEmail(String email) {
        return obtenerReporteGastosPorEmail(email, TipoMoneda.ARS);
    }
    
    @Transactional(readOnly = true)
    public List<GastoPorTipoDTO> obtenerReporteGastosPorEmail(String email, TipoMoneda moneda) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        return transaccionRepository.obtenerTotalPorTipoYCuentaId(cuenta.getId());
    }

    @Transactional(readOnly = true)
    public List<GastoPorCategoriaDTO> obtenerReporteCategoriasPorEmail(String email) {
        return obtenerReporteCategoriasPorEmail(email, TipoMoneda.ARS);
    }
    
    @Transactional(readOnly = true)
    public List<GastoPorCategoriaDTO> obtenerReporteCategoriasPorEmail(String email, TipoMoneda moneda) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        return transaccionRepository.obtenerTotalPorCategoriaYCuentaId(cuenta.getId());
    }

    // Usado por el Asistente IA. Suma todos los egresos desde el día 1 del mes actual.
    @Transactional(readOnly = true)
    public BigDecimal obtenerTotalGastadoEsteMesPorEmail(String email) {
        return obtenerTotalGastadoEsteMesPorEmail(email, TipoMoneda.ARS);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal obtenerTotalGastadoEsteMesPorEmail(String email, TipoMoneda moneda) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), moneda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        LocalDateTime desde = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return transaccionRepository.obtenerTotalEgresosDesde(cuenta.getId(), desde);
    }
}
