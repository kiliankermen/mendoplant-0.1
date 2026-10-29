package com.mendoplant.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Habilita la auditoría automática de entidades (@CreatedBy/@LastModifiedBy/
 * @CreatedDate/@LastModifiedDate): las columnas created_by / created_at /
 * modified_by / modified_at se completan solas con el usuario en sesión.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("sistema");
            }
            try {
                String nombre = auth.getName();
                return Optional.of(nombre == null || nombre.isBlank() ? "sistema" : nombre);
            } catch (RuntimeException e) {
                return Optional.of("sistema");
            }
        };
    }
}