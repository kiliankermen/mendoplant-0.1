package com.mendoplant.app.service;

import com.mendoplant.app.dto.CarritoItemDTO;

import java.math.BigDecimal;
import java.util.List;

/** Carrito de compras de la sesión del usuario (funcional). */
public interface CarritoService {
    List<CarritoItemDTO> items();

    void agregar(Long plantId, int cantidad);

    void quitar(Long plantId);

    void restar(Long plantId);

    void limpiar();

    int cantidadItems();

    BigDecimal total();

    boolean vacio();
}