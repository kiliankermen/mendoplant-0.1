package com.mendoplant.app.controller;

import com.mendoplant.app.dto.CarritoItemDTO;
import com.mendoplant.app.dto.CheckoutItemDTO;
import com.mendoplant.app.dto.EnvioDTO;
import com.mendoplant.app.model.EstadoOrden;
import com.mendoplant.app.model.Order;
import com.mendoplant.app.security.UserPrincipal;
import com.mendoplant.app.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Flujo de compra: se capturan los datos del comprador y del envío, se reserva el
 * stock creando una orden PENDIENTE y se redirige al Checkout de Mercado Pago. Si
 * MP no está configurado (desarrollo), se completa el flujo simulado.
 */
@Controller
@RequestMapping("/ordenes")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PlantService plantService;
    private final PagoService pagoService;
    private final EnvioService envioService;
    private final FacturaService facturaService;
    private final CarritoService carritoService;

    // =====================================================================
    // Checkout unificado (compra directa o carrito) con datos de envío
    // =====================================================================

    @GetMapping("/comprar/{plantId}")
    public String formularioCompraDirecta(@PathVariable Long plantId, Model model) {
        model.addAttribute("modo", "directa");
        model.addAttribute("planta", plantService.buscarPorId(plantId));
        cargarCheckoutBase(model, new EnvioDTO(), "/ordenes/comprar/" + plantId);
        return "orders/checkout";
    }

    @PostMapping("/comprar/{plantId}")
    public String confirmarCompraDirecta(@PathVariable Long plantId, @RequestParam(defaultValue = "1") int cantidad,
                                         @Valid @ModelAttribute("envioDto") EnvioDTO envioDto,
                                         BindingResult result, @AuthenticationPrincipal UserPrincipal principal,
                                         Model model) {
        if (result.hasErrors() || cantidad <= 0) {
            model.addAttribute("modo", "directa");
            model.addAttribute("planta", plantService.buscarPorId(plantId));
            if (result.hasErrors()) {
                model.addAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            }
            cargarCheckoutBase(model, envioDto, "/ordenes/comprar/" + plantId);
            return "orders/checkout";
        }
        try {
            Order orden = orderService.crearOrden(principal.getId(),
                    List.of(new CheckoutItemDTO(plantId, cantidad)));
            envioService.crearEnvio(orden, envioDto);
            return redirigirAPago(orden);
        } catch (RuntimeException ex) {
            model.addAttribute("modo", "directa");
            model.addAttribute("planta", plantService.buscarPorId(plantId));
            cargarCheckoutBase(model, envioDto, "/ordenes/comprar/" + plantId);
            model.addAttribute("error", ex.getMessage());
            return "orders/checkout";
        }
    }

    @GetMapping("/checkout")
    public String formularioCheckoutCarrito(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (carritoService.vacio()) {
            return "redirect:/carrito";
        }
        BigDecimal subtotal = carritoService.total();
        model.addAttribute("modo", "carrito");
        model.addAttribute("items", carritoService.items());
        model.addAttribute("subtotal", subtotal);
        cargarCheckoutBase(model, new EnvioDTO(), "/ordenes/checkout");
        return "orders/checkout";
    }

    @PostMapping("/checkout")
    public String confirmarCheckoutCarrito(@Valid @ModelAttribute("envioDto") EnvioDTO envioDto,
                                           BindingResult result, @AuthenticationPrincipal UserPrincipal principal,
                                           Model model) {
        List<CarritoItemDTO> items = carritoService.items();
        if (result.hasErrors()) {
            model.addAttribute("modo", "carrito");
            model.addAttribute("items", items);
            model.addAttribute("subtotal", carritoService.total());
            cargarCheckoutBase(model, envioDto, "/ordenes/checkout");
            return "orders/checkout";
        }
        try {
            List<CheckoutItemDTO> checkoutItems = items.stream()
                    .map(item -> new CheckoutItemDTO(item.getPlantId(), item.getCantidad()))
                    .toList();
            Order orden = orderService.crearOrden(principal.getId(), checkoutItems);
            envioService.crearEnvio(orden, envioDto);
            carritoService.limpiar();
            return redirigirAPago(orden);
        } catch (RuntimeException ex) {
            model.addAttribute("modo", "carrito");
            model.addAttribute("items", items);
            model.addAttribute("subtotal", carritoService.total());
            cargarCheckoutBase(model, envioDto, "/ordenes/checkout");
            model.addAttribute("error", ex.getMessage());
            return "orders/checkout";
        }
    }

    /** Devuelve el costo de envío para la provincia seleccionada (lo usa el JS del checkout). */
    @GetMapping("/costo-envio")
    @ResponseBody
    public Map<String, BigDecimal> costeEnvio(@RequestParam(required = false) String provincia,
                                              @RequestParam(required = false) String subtotal) {
        BigDecimal subtotalItems = normalizarMonto(subtotal);
        BigDecimal costo = envioService.calcularCosto(provincia, subtotalItems);
        return Map.of("costo", costo);
    }

    // =====================================================================
    // Detalle / historial / reintentar / verificar
    // =====================================================================

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id,
                          @RequestParam(name = "pago", required = false) String pago,
                          @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Order orden = orderService.buscarDelUsuario(id, principal.getId());
        model.addAttribute("orden", orden);
        model.addAttribute("envio", envioService.buscarPorOrden(id));
        model.addAttribute("factura", facturaService.buscarPorOrden(id));
        model.addAttribute("mensajePago", traducirParametroPago(pago));
        return "orders/detail";
    }

    /** Reintenta el pago de una orden que sigue PENDIENTE (pago abandonado o expirado). */
    @PostMapping("/{id}/reintentar")
    public String reintentarPago(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Order orden = orderService.buscarDelUsuario(id, principal.getId());
        if (orden.getEstado() != EstadoOrden.PENDIENTE) {
            return "redirect:/ordenes/" + id;
        }
        return redirigirAPago(orden);
    }

    /** Consulta el estado real del pago en Mercado Pago y actualiza la orden local. */
    @PostMapping("/{id}/verificar")
    public String verificarPago(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        orderService.buscarDelUsuario(id, principal.getId());
        String mensaje = pagoService.verificarYActualizar(id);
        Order orden = orderService.buscarDelUsuario(id, principal.getId());
        model.addAttribute("orden", orden);
        model.addAttribute("envio", envioService.buscarPorOrden(id));
        model.addAttribute("factura", facturaService.buscarPorOrden(id));
        model.addAttribute("mensajePago", mensaje);
        return "orders/detail";
    }

    @GetMapping
    public String historial(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("ordenes", orderService.historialDe(principal.getId()));
        return "orders/history";
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private void cargarCheckoutBase(Model model, EnvioDTO envioDto, String accion) {
        model.addAttribute("envioDto", envioDto);
        model.addAttribute("provincias", Provincias.TODAS);
        model.addAttribute("accion", accion);
    }

    private String redirigirAPago(Order orden) {
        if (pagoService.configurado()) {
            String initPoint = pagoService.crearPreferencia(orden);
            return "redirect:" + initPoint;
        }
        orderService.completarSimulado(orden.getId());
        return "redirect:/ordenes/" + orden.getId() + "?exito=true";
    }

    private String traducirParametroPago(String pago) {
        if (pago == null) {
            return null;
        }
        return switch (pago) {
            case "exito" -> "¡Pago aprobado! Tu compra quedó confirmada.";
            case "pendiente" -> "El pago quedó pendiente. Podés verificar su estado desde esta página.";
            case "fallo" -> "El pago no pudo completarse. Podés reintentarlo o intentar con otro medio.";
            default -> null;
        };
    }

    private BigDecimal normalizarMonto(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}