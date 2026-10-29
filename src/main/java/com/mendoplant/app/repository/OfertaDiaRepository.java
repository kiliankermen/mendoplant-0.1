package com.mendoplant.app.repository;

import com.mendoplant.app.model.OfertaDia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfertaDiaRepository extends JpaRepository<OfertaDia, Long> {

    Optional<OfertaDia> findByDiaSemana(Integer diaSemana);

    /** Oferta del día vigente para un día ISO (1=lunes .. 7=domingo). */
    Optional<OfertaDia> findByDiaSemanaAndActivaTrue(Integer diaSemana);

    List<OfertaDia> findAllByOrderByDiaSemanaAsc();
}