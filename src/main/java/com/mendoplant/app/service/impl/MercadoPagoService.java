package com.mendoplant.app.service.impl;

import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.model.Order;
import com.mendoplant.app.model.OrderItem;
import com.mendoplant.app.service.OrderService;
import com.mendoplant.app.service.PagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integración con Mercado Pago (Checkout Pro) vía su API REST, sin SDK externo.
 *
 * <p>Flujo: se crea una preferencia con los ítems de la orden y el usuario es redirigido al
 * {@code init_point}. El estado real del pago llega por webhook (POST /api/pagos/notificaciones)
 * o se consulta a demanda (GET /v1/payments/search por external_reference).
 *
 * <p>Credenciales por variables de entorno: {@code MP_ACCESS_TOKEN} y {@code MP_SANDBOX=true}.
 */
@Slf4j
@Service
public class MercadoPagoService implements PagoService {

    private static final String API_BASE = "https://api.mercadopago.com";
    private static final String EXTERNAL_REF_PREFIX = "ORDEN-";

    private final RestClient restClient;
    private final OrderService orderService;
    private final String accessToken;
    private final boolean sandbox;
    private final String urlPublica;

    public MercadoPagoService(@Value("${app.mercadopago.access-token:}") String accessToken,
                              @Value("${app.mercadopago.sandbox:true}") boolean sandbox,
                              @Value("${app.url-publica:http://localhost:8080}") String urlPublica,
                              OrderService orderService) {
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.sandbox = sandbox;
        this.urlPublica = urlPublica == null ? "" : urlPublica.trim().replaceAll("/+$", "");
        this.orderService = orderService;
        this.restClient = RestClient.builder().baseUrl(API_BASE).build();
    }

    @Override
    public boolean configurado() {
        return !accessToken.isEmpty();
    }

    @Override
    public String crearPreferencia(Order order) {
        if (!configurado()) {
            throw new BusinessException("Mercado Pago no está configurado (falta MP_ACCESS_TOKEN).");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", itemsDe(order));
        body.put("external_reference", externalReference(order.getId()));
        body.put("statement_descriptor", "MENDOPLANT");
        body.put("auto_return", "approved");
        body.put("back_urls", Map.of(
                "success", urlPublica + "/ordenes/" + order.getId() + "?pago=exito",
                "pending", urlPublica + "/ordenes/" + order.getId() + "?pago=pendiente",
                "failure", urlPublica + "/ordenes/" + order.getId() + "?pago=fallo"));
        body.put("notification_url", urlPublica + "/api/pagos/notificaciones");

        Map<?, ?> respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RuntimeException e) {
            log.error("Error creando preferencia en Mercado Pago para la orden {}", order.getId(), e);
            throw new BusinessException("No se pudo iniciar el pago con Mercado Pago. Intentalo de nuevo.");
        }

        if (respuesta == null) {
            throw new BusinessException("Mercado Pago no devolvió una preferencia.");
        }
        String initPoint = (String) (sandbox
                ? respuesta.get("sandbox_init_point")
                : respuesta.get("init_point"));
        String preferenceId = String.valueOf(respuesta.get("id"));

        orderService.registrarPreferencia(order.getId(), preferenceId);

        if (initPoint == null || initPoint.isBlank()) {
            log.error("Preferencia creada sin init_point: {}", respuesta);
            throw new BusinessException("Mercado Pago no devolvió una URL de pago.");
        }
        return initPoint;
    }

    @Override
    public void procesarNotificacion(Long paymentId) {
        Map<?, ?> pago = getPago(paymentId);
        if (pago != null) {
            procesarPago(pago);
        }
    }

    @Override
    public String verificarYActualizar(Long orderId) {
        Map<?, ?> pago = buscarPagoPorOrden(orderId);
        if (pago == null) {
            return "Todavía no se registró un pago para esta orden en Mercado Pago.";
        }
        procesarPago(pago);
        String estado = str(pago.get("status"));
        return switch (estado) {
            case "approved" -> "El pago fue aprobado y la compra quedó confirmada.";
            case "pending", "in_process", "in_mediation" -> "El pago está pendiente de acreditación. Te avisaremos cuando se confirme.";
            case "rejected" -> "El pago fue rechazado y la orden fue cancelada.";
            case "cancelled" -> "El pago fue cancelado y la orden fue cancelada.";
            default -> "Estado del pago: " + (estado == null ? "desconocido" : estado);
        };
    }

    /** GET /v1/payments/{id}: obtiene el payment y lo aplica a la orden local. */
    private void procesarPago(Map<?, ?> pago) {
        String externalReference = str(pago.get("external_reference"));
        Long orderId = orderIdDesdeReferencia(externalReference);
        if (orderId == null) {
            log.warn("Webhook MP sin external_reference válida ({}).", externalReference);
            return;
        }
        Order order = orderService.buscarPorId(orderId);
        verificarMonto(pago, order);

        String estado = str(pago.get("status"));
        Long paymentId = numeroLong(pago.get("id"));

        switch (estado) {
            case "approved" -> orderService.registrarPagoAprobado(orderId, paymentId, estado);
            case "pending", "in_process", "in_mediation" ->
                    orderService.registrarPagoPendiente(orderId, paymentId, estado);
            case "rejected", "cancelled" -> orderService.registrarPagoFallido(orderId);
            default -> log.warn("Estado MP no manejado para la orden {}: {}", orderId, estado);
        }
    }

    private Map<?, ?> getPago(Long paymentId) {
        try {
            return restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RuntimeException e) {
            log.error("Error consultando payment {} en Mercado Pago", paymentId, e);
            return null;
        }
    }

    private Map<?, ?> buscarPagoPorOrden(Long orderId) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(uri -> uri.path("/v1/payments/search")
                            .queryParam("external_reference", externalReference(orderId))
                            .queryParam("sort", "date_created")
                            .queryParam("criteria", "desc")
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                return null;
            }
            Object results = response.get("results");
            if (results instanceof List<?> lista && !lista.isEmpty()) {
                Object first = lista.get(0);
                return first instanceof Map<?, ?> m ? m : null;
            }
            return null;
        } catch (RuntimeException e) {
            log.error("Error buscando payment para la orden {}", orderId, e);
            return null;
        }
    }

    /** Idempotencia y defensa: el monto cobrado debe coincidir con el total de la orden. */
    private void verificarMonto(Map<?, ?> pago, Order order) {
        Object rawAmount = pago.get("transaction_amount");
        if (!(rawAmount instanceof Number number)) {
            throw new BusinessException("Respuesta de Mercado Pago sin transaction_amount.");
        }
        BigDecimal cobrado = BigDecimal.valueOf(number.doubleValue())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal esperado = order.getTotal().setScale(2, RoundingMode.HALF_UP);
        if (cobrado.compareTo(esperado) != 0) {
            log.error("Monto no coincide para la orden {}: cobrado={} esperado={}",
                    order.getId(), cobrado, esperado);
            throw new BusinessException("El monto del pago no coincide con la orden.");
        }
    }

    private List<Map<String, Object>> itemsDe(Order order) {
        List<Map<String, Object>> items = new ArrayList<>();
        int i = 0;
        for (OrderItem item : order.getItems()) {
            String titulo = item.getPlant().getNombreComun();
            if (titulo != null && titulo.length() > 80) {
                titulo = titulo.substring(0, 77) + "...";
            }
            Map<String, Object> mpItem = new LinkedHashMap<>();
            mpItem.put("id", "MNDPLT-" + order.getId() + "-" + i++);
            mpItem.put("title", titulo);
            mpItem.put("quantity", item.getCantidad());
            mpItem.put("unit_price", item.getPrecioUnitario().doubleValue());
            mpItem.put("currency_id", "ARS");
            items.add(mpItem);
        }
        // El costo de envío se cobra junto con los productos en la misma preferencia.
        if (order.getEnvio() != null && order.getEnvio().getCosto() != null
                && order.getEnvio().getCosto().signum() > 0) {
            Map<String, Object> envioItem = new LinkedHashMap<>();
            envioItem.put("id", "MNDENV-" + order.getId());
            envioItem.put("title", "Envío a " + order.getEnvio().getProvincia());
            envioItem.put("quantity", 1);
            envioItem.put("unit_price", order.getEnvio().getCosto().doubleValue());
            envioItem.put("currency_id", "ARS");
            items.add(envioItem);
        }
        return items;
    }

    private String externalReference(Long orderId) {
        return EXTERNAL_REF_PREFIX + orderId;
    }

    private Long orderIdDesdeReferencia(String externalReference) {
        if (externalReference == null || !externalReference.startsWith(EXTERNAL_REF_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(externalReference.substring(EXTERNAL_REF_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long numeroLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof CharSequence s) {
            try {
                return Long.parseLong(s.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}