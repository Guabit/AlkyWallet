package com.alkywallet.repository;


import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.TipoMoneda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    List<Cuenta> findByUsuarioId(Long usuarioId);
    Optional<Cuenta> findByUsuarioIdAndTipoMoneda(Long usuarioId, TipoMoneda tipoMoneda);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.id = :id")
    Optional<Cuenta> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.usuario.id = :userId AND c.tipoMoneda = :moneda")
    Optional<Cuenta> findByUsuarioIdAndTipoMonedaForUpdate(@Param("userId") Long userId, @Param("moneda") TipoMoneda moneda);
}
