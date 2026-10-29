package com.mendoplant.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ítem del carrito persistente: vincula un producto con un usuario y su cantidad.
 * Se usa para recuperar el carrito armado aunque la sesión HTTP se cierre.
 */
@Entity
@Table(name = "carrito_item", uniqueConstraints = @UniqueConstraint(
        name = "uq_carrito_usuario_planta", columnNames = {"usuario_id", "planta_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarritoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planta_id", nullable = false)
    private Plant planta;

    @Column(nullable = false)
    private Integer cantidad;
}