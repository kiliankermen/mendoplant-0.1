package com.mendoplant.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Promoción de descuento por porcentaje, administrable desde el panel de ADMIN.
 * <p>
 * Una promoción es una regla de descuento:
 * <ul>
 *   <li><b>porcentaje</b>: descuento aplicado (1 a 100).</li>
 *   <li><b>diasSemana</b>: días en que aplica ("1,3,5" con ISO 1=lunes..7=domingo;
 *       vacío = todos los días).</li>
 *   <li><b>fechaInicio/fechaFin</b>: ventana de vigencia (opcional).</li>
 *   <li><b>tipoAplicacion</b>: TODAS | CATEGORIA | PLANTAS.</li>
 *   <li><b>clima</b>: filtro opcional adicional (p. ej. TROPICAL).</li>
 * </ul>
 * El mejor descuento vigente de cada planta es el que se aplica (siempre un
 * porcentaje, nunca se acumulan).
 */
@Entity
@Table(name = "promociones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la promoción es obligatorio")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombre;

    /** Texto breve que se muestra en el banner de la portada. Opcional. */
    @Size(max = 100)
    @Column(length = 100)
    private String descripcion;

    @NotNull(message = "El porcentaje de descuento es obligatorio")
    @DecimalMin(value = "1", message = "El porcentaje debe ser mayor o igual a 1")
    @DecimalMax(value = "100", message = "El porcentaje no puede superar 100")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    /** Color hexadecimal del banner en la portada (ej. #2f7d45). Opcional. */
    @Size(max = 7)
    @Column(name = "color_banner", length = 7)
    private String colorBanner;

    /** Días de la semana ISO (1=lunes..7=domingo) separados por coma. Vacío = todos los días. */
    @Column(name = "dias_semana", nullable = false, length = 20)
    @Builder.Default
    private String diasSemana = "";

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @NotNull(message = "Debés elegir el alcance de la promoción")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_aplicacion", nullable = false, length = 20)
    private TipoAplicacion tipoAplicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Category categoria;

    /** Filtro opcional por clima: si está seteado, solo aplica a plantas de ese clima. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoClima clima;

    @ManyToMany
    @JoinTable(
            name = "promocion_plantas",
            joinColumns = @JoinColumn(name = "promo_id"),
            inverseJoinColumns = @JoinColumn(name = "planta_id")
    )
    @Builder.Default
    private Set<Plant> plantas = new HashSet<>();

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