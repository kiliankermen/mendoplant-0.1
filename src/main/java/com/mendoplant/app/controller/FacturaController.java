package com.mendoplant.app.controller;

import com.mendoplant.app.model.Factura;
import com.mendoplant.app.model.Order;
import com.mendoplant.app.security.UserPrincipal;
import com.mendoplant.app.service.FacturaService;
import com.mendoplant.app.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Vista de comprobantes fiscales. La factura solo la puede ver el comprador de la
 * orden (o un admin). Es una vista imprimible/A4.
 */
@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final OrderService orderService;

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Factura factura = facturaService.buscarPorId(id);
        Order orden = orderService.buscarDelUsuario(factura.getOrden().getId(), principal.getId());
        model.addAttribute("factura", factura);
        model.addAttribute("orden", orden);
        return "facturas/detalle";
    }
}