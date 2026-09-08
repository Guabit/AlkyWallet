package com.alkywallet.repository;

import com.alkywallet.entity.PagoMercadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoMercadoPagoRepository extends JpaRepository<PagoMercadoPago, Long> {
    boolean existsByMercadoPagoPaymentId(String mercadoPagoPaymentId);
}
