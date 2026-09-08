package com.alkywallet.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.alkywallet.entity.Inversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InversionRepository extends JpaRepository<Inversion, Long> {
    List<Inversion> findByCuentaIdOrderByFechaInicioDesc(Long cuentaId);
    List<Inversion> findByCuentaIdAndActivaTrue(Long cuentaId);
    Optional<Inversion> findByIdAndCuentaId(Long id, Long cuentaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inversion i WHERE i.id = :id")
    Optional<Inversion> findByIdForUpdate(@Param("id") Long id);
}
