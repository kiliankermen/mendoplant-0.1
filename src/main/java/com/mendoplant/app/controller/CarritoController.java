package com.mendoplant.app.controller;

import com.mendoplant.app.dto.CarritoItemDTO;
import com.mendoplant.app.service.CarritoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrito de la sesión: agregar/quitar ítems. La finalización del pedido se hace
 * en el checkout (GET/POST /ordenes/checkout) para capturar los datos de envío.
 */
@Controller
@RequestMapping("/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    public String verCarrito(Model model) {
        model.addAttribute("items", carritoService.items());
        model.addAttribute("total", carritoService.total());
        return "orders/cart";
    }

    @PostMapping("/agregar")
    public ResponseEntity<?> agregar(@RequestParam Long plantId,
                                     @RequestParam(defaultValue = "1") int cantidad,
                                     @RequestHeader(value = "referer", required = false) String referer,
                                     HttpServletRequest request) {
        carritoService.agregar(plantId, cantidad);
        if ("fetch".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            // Agregado vía AJAX: se devuelve el estado del carrito para que el
            // cliente actualice el badge y los totales sin recargar la página.
            return ResponseEntity.ok(datosCarrito());
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(destinoSeguro(request, referer)))
                .build();
    }

    /** Menú desplegable del carrito ya renderizado, para refrescarlo vía AJAX. */
    @GetMapping(path = "/menu", produces = "text/html")
    public String menuCarrito() {
        return "fragments/layout :: cartMenu";
    }

    @PostMapping("/quitar/{plantId}")
    public ResponseEntity<?> quitar(@PathVariable Long plantId, HttpServletRequest request) {
        carritoService.quitar(plantId);
        if ("fetch".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return ResponseEntity.ok(datosCarrito());
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/carrito"))
                .build();
    }

    @PostMapping("/restar/{plantId}")
    public String restar(@PathVariable Long plantId) {
        carritoService.restar(plantId);
        return "redirect:/carrito";
    }

    @PostMapping("/vaciar")
    public String vaciar() {
        carritoService.limpiar();
        return "redirect:/carrito";
    }

    private Map<String, Object> datosCarrito() {
        List<Map<String, Object>> items = carritoService.items().stream()
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("plantId", i.getPlantId());
                    m.put("cantidad", i.getCantidad());
                    m.put("subtotal", i.getSubtotal().toPlainString());
                    return m;
                })
                .toList();
        Map<String, Object> datos = new HashMap<>();
        datos.put("cantidad", carritoService.cantidadItems());
        datos.put("total", carritoService.total().toPlainString());
        datos.put("items", items);
        return datos;
    }

    private String destinoSeguro(HttpServletRequest request, String referer) {
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        if (referer.startsWith("/")) {
            return referer.startsWith("//") || referer.startsWith("/\\") ? "/" : referer;
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getScheme() != null
                    && uri.getHost() != null
                    && uri.getScheme().equalsIgnoreCase(request.getScheme())
                    && uri.getHost().equalsIgnoreCase(request.getServerName())
                    && (uri.getPort() == -1 || uri.getPort() == request.getServerPort())) {
                String destino = uri.getRawPath() == null ? "/carrito" : uri.getRawPath();
                return uri.getRawQuery() == null ? destino : destino + "?" + uri.getRawQuery();
            }
        } catch (IllegalArgumentException ignored) {
        }
        return "/";
    }
}