package com.alkywallet.repository;

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
}
