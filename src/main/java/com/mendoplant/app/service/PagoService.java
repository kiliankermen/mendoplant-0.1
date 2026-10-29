package com.mendoplant.app.service;

import com.mendoplant.app.model.Order;

public interface PagoService {

    /** True si hay access token de Mercado Pago configurado (se lee de env). */
    boolean configurado();

    /** Crea la preferencia (Checkout Pro) para la orden y devuelve la URL de pago (init_point). */
    String crearPreferencia(Order order);

    /** Procesa una notificación de Mercado Pago (webhook) con el payment id recibido. */
    void procesarNotificacion(Long paymentId);

    /**
     * Consulta el estado real del pago de una orden en Mercado Pago, actualiza la orden
     * localmente y devuelve un mensaje legible para el usuario.
     */
    String verificarYActualizar(Long orderId);
}