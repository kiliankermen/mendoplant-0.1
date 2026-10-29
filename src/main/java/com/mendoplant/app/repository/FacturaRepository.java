package com.mendoplant.app.repository;

import com.mendoplant.app.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findFirstByOrdenIdOrderByIdDesc(Long ordenId);

    long countByLetra(String letra);
}