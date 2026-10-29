package com.mendoplant.app.repository;

import com.mendoplant.app.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    /** Promociones activas (no desactivadas lógicamente). */
    List<Promocion> findByActivaTrue();

    /** Rebajas activas creadas por un vendedor (created_by = email). */
    List<Promocion> findByActivaTrueAndCreatedBy(String createdBy);

    /** Rebaja propia de un vendedor (se reutiliza para no duplicar filas). */
    Optional<Promocion> findFirstByNombreAndActivaTrueAndCreatedBy(String nombre, String createdBy);
}