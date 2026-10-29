package com.mendoplant.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Oferta del día rotativa: una planta destacada por día de la semana con su
 * porcentaje de descuento. Dia de semana ISO (1=lunes .. 7=domingo), único.
 * El ADMIN decide qué planta toca cada día y con qué descuento.
 */
@Entity
@Table(name = "ofertas_dia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OfertaDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El día de la semana es obligatorio")
    @Min(value = 1, message = "Día inválido (1=lunes)")
    @Max(value = 7, message = "Día inválido (7=domingo)")
    @Column(name = "dia_semana", nullable = false, unique = true)
    private Integer diaSemana;

    @NotNull(message = "Debés elegir la planta de la oferta")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planta_id", nullable = false)
    private Plant planta;

    @NotNull(message = "El porcentaje de descuento es obligatorio")
    @DecimalMin(value = "1", message = "El porcentaje debe ser mayor o igual a 1")
    @DecimalMax(value = "100", message = "El porcentaje no puede superar 100")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

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