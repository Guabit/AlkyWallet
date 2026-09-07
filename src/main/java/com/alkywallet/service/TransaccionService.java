package com.alkywallet.service;

import com.alkywallet.dto.GastoPorTipoDTO;
import com.alkywallet.dto.TransaccionDTO;
import com.alkywallet.exception.ResourceNotFoundException;
import com.alkywallet.exception.SaldoInsuficienteException;
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
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), TipoMoneda.ARS)
                .orElseGet(() -> {
                    Cuenta nueva = Cuenta.builder()
                            .usuario(usuario)
                            .saldo(BigDecimal.ZERO)
                            .tipoMoneda(TipoMoneda.ARS)
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

        Cuenta cuenta = cuentaRepository.findById(cuentaId)
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
        if (emailOrigen.trim().equalsIgnoreCase(destinatarioEmail.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No podés transferirte dinero a vos mismo");
        }

        Usuario usuarioOrigen = userRepository.findByEmail(emailOrigen)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario origen no encontrado"));

        Cuenta cuentaOrigen = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuarioOrigen.getId(), TipoMoneda.ARS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta origen no encontrada"));

        Usuario usuarioDestino = userRepository.findByEmail(destinatarioEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un usuario con el email: " + destinatarioEmail));

        Cuenta cuentaDestino = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuarioDestino.getId(), TipoMoneda.ARS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El destinatario no posee una cuenta activa"));

        realizarTransferencia(cuentaOrigen.getId(), cuentaDestino.getId(), monto);
    }

    @Transactional
    public void realizarTransferencia(Long cuentaOrigenId, Long cuentaDestinoId, Double monto) {
        if (cuentaOrigenId == null || cuentaDestinoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las cuentas de origen y destino son obligatorias");
        }
        if (cuentaOrigenId.equals(cuentaDestinoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta destino debe ser distinta a la cuenta origen");
        }
        if (monto == null || monto <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
        }

        Cuenta cuentaOrigen = cuentaRepository.findById(cuentaOrigenId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta origen no encontrada"));

        Cuenta cuentaDestino = cuentaRepository.findById(cuentaDestinoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta destino no encontrada"));

        if (cuentaOrigen.isDeleted() || cuentaDestino.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede transferir desde o hacia una cuenta inactiva");
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
                .cuenta(cuentaDestino)
                .build();
        transaccionRepository.save(ingreso);
    }

    @Transactional(readOnly = true)
    public List<TransaccionDTO> obtenerHistorialPorEmail(String email) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), TipoMoneda.ARS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        return transaccionRepository.obtenerHistorialPorCuentaId(cuenta.getId());
    }

    @Transactional(readOnly = true)
    public List<GastoPorTipoDTO> obtenerReporteGastosPorEmail(String email) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), TipoMoneda.ARS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        return transaccionRepository.obtenerTotalPorTipoYCuentaId(cuenta.getId());
    }
}