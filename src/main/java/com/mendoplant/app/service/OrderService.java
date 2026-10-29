package com.mendoplant.app.service;

import com.mendoplant.app.dto.CheckoutItemDTO;
import com.mendoplant.app.model.EstadoOrden;
import com.mendoplant.app.model.Order;

import java.util.List;

public interface OrderService {

    /** Crea una orden en estado PENDIENTE reservando el stock. No confirma el pago. */
    Order crearOrden(Long compradorId, List<CheckoutItemDTO> items);

    List<Order> historialDe(Long compradorId);

    Order buscarPorId(Long id);

    /** Busca una orden verificando que pertenezca al usuario (o que sea admin). */
    Order buscarDelUsuario(Long id, Long usuarioId);

    /** Marca la orden como COMPLETADA cuando Mercado Pago confirma el cobro. */
    void registrarPagoAprobado(Long orderId, Long paymentId, String mpStatus);

    /** El pago quedó pendiente: guarda la referencia para reintentos/verificación. */
    void registrarPagoPendiente(Long orderId, Long paymentId, String mpStatus);

    /** Guarda el id de la preferencia de Mercado Pago recién creada. */
    void registrarPreferencia(Long orderId, String preferenceId);

    /** El pago fue rechazado o cancelado: cancela la orden y repone el stock reservado. */
    void registrarPagoFallido(Long orderId);

    /** Flujo simulado (sin Mercado Pago configurado): completa la orden directamente. */
    void completarSimulado(Long orderId);

    // ----- Panel de administración -----

    List<Order> listarTodas();

    long contarPorEstado(EstadoOrden estado);

    /** Cambia el estado de una orden desde el admin (solo PENDIENTE -> CANCELADO). */
    void cambiarEstado(Long orderId, EstadoOrden nuevoEstado);
}