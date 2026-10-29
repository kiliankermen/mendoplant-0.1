package com.mendoplant.app.repository;

import com.mendoplant.app.model.CareGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareGuideRepository extends JpaRepository<CareGuide, Long> {

    /** Guías vigentes (no eliminadas lógicamente). */
    List<CareGuide> findByActivoTrueOrderByTituloFichaAsc();
}
