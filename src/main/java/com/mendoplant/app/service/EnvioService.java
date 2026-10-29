package com.mendoplant.app.service;

import com.mendoplant.app.dto.EnvioDTO;
import com.mendoplant.app.model.Envio;
import com.mendoplant.app.model.EstadoEnvio;
import com.mendoplant.app.model.Order;

import java.math.BigDecimal;

public interface EnvioService {

    /** Calcula el costo de envío según provincia y subtotal (gratis desde un umbral configurable). */
    BigDecimal calcularCosto(String provincia, BigDecimal subtotalItems);

    /** Crea el envío de una orden, suma el costo al total de la orden y la persiste. */
    Envio crearEnvio(Order order, EnvioDTO dto);

    Envio buscarPorOrden(Long ordenId);

    /** Actualiza el estado de despacho (y transportista/tracking). */
    void actualizarEstado(Long envioId, EstadoEnvio estado, String transportista, String tracking);
}