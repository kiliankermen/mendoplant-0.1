package com.mendoplant.app.repository;

import com.mendoplant.app.model.VerificacionEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacionEmailRepository extends JpaRepository<VerificacionEmail, Long> {

    Optional<VerificacionEmail> findFirstByEmailAndUsadaFalseOrderByCreatedAtDesc(String email);
}