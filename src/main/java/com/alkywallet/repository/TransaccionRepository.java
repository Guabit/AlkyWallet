package com.alkywallet.repository;

import com.alkywallet.dto.GastoPorCategoriaDTO;
import com.alkywallet.dto.GastoPorTipoDTO;
import com.alkywallet.dto.TransaccionDTO;
import com.alkywallet.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    @Query("""
        SELECT new com.alkywallet.dto.TransaccionDTO(
            t.id,
            t.monto,
            t.fecha,
            t.tipo,
            t.concepto,
            t.cuenta.id,
            t.categoria
        )
        FROM Transaccion t
        WHERE t.cuenta.id = :cuentaId
        ORDER BY t.fecha DESC
    """)
    List<TransaccionDTO> obtenerHistorialPorCuentaId(@Param("cuentaId") Long cuentaId);

    @Query("""
        SELECT new com.alkywallet.dto.GastoPorTipoDTO(
            t.tipo,
            SUM(t.monto)
        )
        FROM Transaccion t
        JOIN t.cuenta c
        WHERE c.id = :cuentaId
        GROUP BY t.tipo
    """)
    List<GastoPorTipoDTO> obtenerTotalPorTipoYCuentaId(@Param("cuentaId") Long cuentaId);

    @Query("""
        SELECT new com.alkywallet.dto.GastoPorCategoriaDTO(
            t.categoria,
            SUM(t.monto)
        )
        FROM Transaccion t
        JOIN t.cuenta c
        WHERE c.id = :cuentaId
        GROUP BY t.categoria
    """)
    List<GastoPorCategoriaDTO> obtenerTotalPorCategoriaYCuentaId(@Param("cuentaId") Long cuentaId);

    // Usado por el Asistente IA para responder "¿cuánto gasté este mes?".
    @Query("""
        SELECT COALESCE(SUM(t.monto), 0)
        FROM Transaccion t
        WHERE t.cuenta.id = :cuentaId
          AND t.tipo = com.alkywallet.entity.TipoTransaccion.EGRESO
          AND t.fecha >= :desde
    """)
    BigDecimal obtenerTotalEgresosDesde(@Param("cuentaId") Long cuentaId, @Param("desde") LocalDateTime desde);
}
