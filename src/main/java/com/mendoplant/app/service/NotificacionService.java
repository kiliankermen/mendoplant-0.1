package com.mendoplant.app.service;

import com.mendoplant.app.dto.NotificacionDTO;
import com.mendoplant.app.model.Plant;

import java.util.List;

/**
 * Bandeja de notificaciones del usuario en sesión, persistidas en base de datos:
 * avisos de promos activas, compras confirmadas y bienvenida.
 */
public interface NotificacionService {
    List<NotificacionDTO> notificacionesPara(Long usuarioId);

    long noLeidas(Long usuarioId);

    void marcarTodasComoLeidas(Long usuarioId);

    boolean silenciadas(Long usuarioId);

    boolean alternarSilencio(Long usuarioId);

    void eliminar(Long usuarioId, Long notificacionId);

    void eliminarTodas(Long usuarioId);

    void crear(Long usuarioId, String icono, String titulo, String mensaje);

    /** Crea una notificación opcionalmente vinculada a una planta (favorito con descuento). */
    void crear(Long usuarioId, String icono, String titulo, String mensaje, Plant planta);
}