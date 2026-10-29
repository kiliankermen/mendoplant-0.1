package com.mendoplant.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Ficha de cuidado de una planta o de una categoría de plantas.
 * Puede asociarse a una Plant específica, o dejarse genérica para una categoría (por nombre).
 * Eliminación lógica mediante {@code activo}.
 */
@Entity
@Table(name = "guias_cuidado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CareGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "titulo_ficha", nullable = false, length = 150)
    private String tituloFicha;

    /** Planta específica a la que aplica esta ficha (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planta_id")
    private Plant plant;

    /** Categoría genérica a la que aplica, si no está atada a una planta puntual. */
    @Column(length = 60)
    private String categoriaGenerica;

    @Column(name = "frecuencia_riego", length = 150)
    private String frecuenciaRiego;

    @Column(length = 150)
    private String humedad;

    @Column(length = 150)
    private String temperatura;

    @Lob
    private String recomendaciones;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_by", length = 100)
    @CreatedBy
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "modified_by", length = 100)
    @LastModifiedBy
    private String modifiedBy;

    @Column(name = "modified_at")
    @LastModifiedDate
    private LocalDateTime modifiedAt;
}
