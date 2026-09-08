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
 * MOCK de integración con PedidosYa. No llama a ninguna API externa real
 * (no tenemos credenciales): simula una pasarela de pago de delivery,
 * pero SÍ debita el saldo real dentro de AlkyWallet y deja registrada la
 * transacción, para que la funcionalidad se pueda demostrar de punta a
 * punta. Cuando consigan acceso a la API real, este service se reemplaza
 * por un PedidosYaApiService que hable con la API real, sin tocar el
 * controller ni el resto de la app.
 */
@Service
@RequiredArgsConstructor
public class PedidosYaMockService {

    private final CuentaRepository cuentaRepository;
    private final UserRepository userRepository;
    private final TransaccionRepository transaccionRepository;

    @Transactional
    public PagoServicioResponseDTO pagarPedido(String email, String comercio, Double monto) {
        Usuario usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Cuenta cuenta = cuentaRepository.findByUsuarioIdAndTipoMoneda(usuario.getId(), TipoMoneda.ARS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        BigDecimal montoBigDecimal = BigDecimal.valueOf(monto);

        if (cuenta.getSaldo().compareTo(montoBigDecimal) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para este pago");
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(montoBigDecimal));
        cuentaRepository.save(cuenta);

        // Referencia simulada, como si fuera el número de orden que devolvería la API real.
        String referencia = "PEDIDOSYA-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaccion transaccion = Transaccion.builder()
                .monto(montoBigDecimal)
                .fecha(LocalDateTime.now())
                .tipo(TipoTransaccion.EGRESO)
                .concepto("Pago PedidosYa (simulado) - " + comercio)
                .categoria(CategoriaTransaccion.COMIDA)
                .cuenta(cuenta)
                .build();
        transaccionRepository.save(transaccion);

        return new PagoServicioResponseDTO(referencia, "APROBADO", montoBigDecimal);
    }
}
