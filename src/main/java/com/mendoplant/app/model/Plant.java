package com.mendoplant.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Producto del catálogo: una planta publicada para la venta por un usuario/vendedor.
 * La "oferta del día" no es un flag de la planta: se administra en {@literal ofertas_dia}.
 */
@Entity
@Table(name = "plantas")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank(message = "El nombre común es obligatorio")
    @Size(max = 120)
    @Column(name = "nombre_comun", nullable = false, length = 120)
    @ToString.Include
    private String nombreComun;

    @Size(max = 150)
    @Column(name = "nombre_cientifico", length = 150)
    private String nombreCientifico;

    @Size(max = 2000)
    @Column(length = 2000)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    /** Galería completa del producto (la primera imagen también se guarda en imagenUrl). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_imagenes", joinColumns = @JoinColumn(name = "planta_id"))
    @OrderColumn(name = "orden")
    @Column(name = "url", length = 500, nullable = false)
    @Builder.Default
    private List<String> imagenes = new ArrayList<>();

    @Column(name = "fecha_publicacion", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaPublicacion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Category categoria;

    /**
     * Cada atributo admite varias etiquetas: una planta puede ser, por
     * ejemplo, de ambiente INTERIOR y EXTERIOR a la vez. Cada conjunto se
     * guarda en su propia tabla puente (plantas_ambiente, plantas_clima, ...).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_ambiente", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<Ambiente> ambientes = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_clima", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<TipoClima> climas = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_luz", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<TipoLuz> luces = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_nivel_cuidado", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<NivelCuidado> nivelesCuidado = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_tipo_planta", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<TipoPlanta> tiposPlanta = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_tamano", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<Tamano> tamanos = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_tipo_maceta", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<TipoMaceta> tiposMaceta = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_tipo_sustrato", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<TipoSustrato> tiposSustrato = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plantas_tipo_accesorio", joinColumns = @JoinColumn(name = "planta_id"))
    @Column(name = "valor", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 60)
    @Builder.Default
    private Set<TipoAccesorio> tiposAccesorio = new LinkedHashSet<>();

    @Column(name = "es_combo", nullable = false)
    @Builder.Default
    private boolean esCombo = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private User vendedor;

    /** Marca lógica para no listar plantas que un vendedor decidió retirar. */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Cantidad de unidades vendidas (se incrementa con cada pago aprobado). */
    @Column(nullable = false)
    @Builder.Default
    private Integer ventas = 0;

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
