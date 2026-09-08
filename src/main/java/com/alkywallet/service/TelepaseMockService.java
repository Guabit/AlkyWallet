package com.alkywallet.service;

import com.alkywallet.dto.PagoServicioResponseDTO;
import com.alkywallet.entity.CategoriaTransaccion;
import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.entity.TipoTransaccion;
import com.alkywallet.entity.Transaccion;
import com.alkywallet.entity.Usuario;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.TransaccionRepository;
import com.alkywallet.repository.UserRepository;
import com.alkywallet.exception.SaldoInsuficienteException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MOCK de integración con Telepase. Misma idea que PedidosYaMockService:
 * sin credenciales reales todavía, pero con el débito de saldo real
 * dentro de la wallet para poder demostrar el flujo completo. Reemplazar
 * por TelepaseApiService cuando haya acceso a la API oficial.
 */
@Service
@RequiredArgsConstructor
public class TelepaseMockService {

    private final CuentaRepository cuentaRepository;
    private final UserRepository userRepository;
    private final TransaccionRepository transaccionRepository;

    @Transactional
    public PagoServicioResponseDTO pagarPeaje(String email, Double monto, String patente) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMonedaForUpdate(usuario.getId(), TipoMoneda.ARS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        BigDecimal montoBigDecimal = BigDecimal.valueOf(monto);

        if (cuenta.getSaldo().compareTo(montoBigDecimal) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para este pago");
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(montoBigDecimal));
        cuentaRepository.save(cuenta);

        String referencia = "TELEPASE-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String detallePatente = (patente != null && !patente.isBlank()) ? " (" + patente.toUpperCase() + ")" : "";

        Transaccion transaccion = Transaccion.builder()
                .monto(montoBigDecimal)
                .fecha(LocalDateTime.now())
                .tipo(TipoTransaccion.EGRESO)
                .concepto("Pago Telepase (simulado)" + detallePatente)
                .categoria(CategoriaTransaccion.TRANSPORTE)
                .cuenta(cuenta)
                .build();
        transaccionRepository.save(transaccion);

        return new PagoServicioResponseDTO(referencia, "APROBADO", montoBigDecimal);
    }
}
