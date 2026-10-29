package com.mendoplant.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos de envío de una orden: dirección de entrega, costo calculado y estado del
 * despacho (lo gestiona el admin). Cada orden tiene como máximo un envío.
 */
@Entity
@Table(name = "envios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "orden")
@EntityListeners(AuditingEntityListener.class)
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false, unique = true)
    private Order orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE;

    @Column(nullable = false, length = 120)
    private String receptorNombre;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 200)
    private String calleNumero;

    @Column(length = 50)
    private String depto;

    @Column(nullable = false, length = 80)
    private String ciudad;

    @Column(length = 12)
    private String codigoPostal;

    @Column(nullable = false, length = 60)
    private String provincia;

    @Column(length = 300)
    private String instrucciones;

    /** Costo del envío. Ya está incluido en Order.total. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    /** Correo/empresa de despacho (lo carga el admin). */
    @Column(length = 60)
    private String transportista;

    /** Código de seguimiento (lo carga el admin). */
    @Column(length = 100)
    private String tracking;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Fecha en la que se despachó (se setea al pasar a ENVIADO). */
    private LocalDateTime fechaEnvio;

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