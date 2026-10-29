package com.mendoplant.app.model;

import jakarta.persistence.*;
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
 * Comprobante fiscal (factura) emitido por el vendedor cuando una orden queda
 * pagada. En modo local se genera un CFE numerado y descargable/exportable; la
 * integración con el WS de AFIP (WSAA + WSFEv1) queda configurada por
 * {@code app.facturacion.proveedor} y requiere certificado fiscal de la empresa.
 */
@Entity
@Table(name = "facturas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Orden asociada. Puede haber varias facturas por orden (una anulada y su reemisión). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Order orden;

    /** Número del comprobante: "{punto de venta:5}-{número:8}". */
    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = false)
    private Integer puntoVenta;

    @Column(nullable = false, length = 1)
    private String letra;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    // Datos del emisor (se leen de la configuración de facturación).
    @Column(length = 11)
    private String cuitEmisor;

    @Column(nullable = false, length = 120)
    private String razonSocial;

    @Column(length = 200)
    private String domicilioEmisor;

    @Column(length = 80)
    private String condicionFiscalEmisor;

    // Datos del receptor.
    @Column(nullable = false, length = 120)
    private String nombreReceptor;

    /** CUIT/DNI del receptor si lo tiene; "Consumidor Final" si no. */
    @Column(length = 80)
    private String documentoReceptor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "FACTURADA";

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