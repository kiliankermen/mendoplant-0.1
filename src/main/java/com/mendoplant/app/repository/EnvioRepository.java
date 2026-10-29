package com.mendoplant.app.repository;

import com.mendoplant.app.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByOrdenId(Long ordenId);
}