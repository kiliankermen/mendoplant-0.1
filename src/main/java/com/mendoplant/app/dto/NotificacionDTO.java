package com.mendoplant.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Notificación mostrada en la bandeja del header. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionDTO {
    private Long id;
    private String icono;
    private String titulo;
    private String mensaje;
    private LocalDateTime fecha;
    private boolean leida;
    private Long plantaId;
}