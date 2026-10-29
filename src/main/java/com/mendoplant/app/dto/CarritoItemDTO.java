package com.mendoplant.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Ítem del carrito de compras de la sesión. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarritoItemDTO {
    private Long plantId;
    private String nombre;
    private String plantaNombreCientifico;
    private String imagenUrl;
    private BigDecimal precioUnitario;
    private BigDecimal precioDescontado;
    private Integer cantidad;
    private BigDecimal subtotal;

    public BigDecimal getPrecioMostrado() {
        return precioDescontado != null ? precioDescontado : precioUnitario;
    }
}