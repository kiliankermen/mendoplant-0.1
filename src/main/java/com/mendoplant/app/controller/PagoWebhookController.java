package com.mendoplant.app.controller;

import com.mendoplant.app.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook de Mercado Pago. El endpoint es público (sin sesión): la verificación
 * real se hace consultando la API de MP con el {@code payment id} recibido, no
 * confiando ciegamente en el payload. Debe ser alcanzable desde internet
 * (notification_url en la preferencia).
 */
@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoWebhookController {

    private final PagoService pagoService;

    @PostMapping("/notificaciones")
    public ResponseEntity<Void> notificaciones(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            return ResponseEntity.ok().build();
        }
        String tipo = body.get("type") == null ? null : String.valueOf(body.get("type"));
        if (!"payment".equals(tipo)) {
            return ResponseEntity.ok().build();
        }
        Object data = body.get("data");
        Long paymentId = null;
        if (data instanceof Map<?, ?> map && map.get("id") != null) {
            try {
                paymentId = Long.valueOf(String.valueOf(map.get("id")));
            } catch (NumberFormatException e) {
                log.warn("Webhook MP con payment id inválido: {}", map.get("id"));
            }
        }
        if (paymentId == null) {
            log.warn("Webhook MP sin payment id en el payload.");
            return ResponseEntity.ok().build();
        }
        log.info("Notificación de pago Mercado Pago recibida: paymentId={}", paymentId);
        pagoService.procesarNotificacion(paymentId);
        return ResponseEntity.ok().build();
    }
}