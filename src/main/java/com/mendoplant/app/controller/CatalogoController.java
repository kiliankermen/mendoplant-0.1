package com.mendoplant.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Compatibilidad: el catálogo completo se sirve ahora en /plantas.
 * /catalogo se mantiene y redirige a la nueva ruta para no romper vínculos viejos.
 */
@Controller
public class CatalogoController {

    @GetMapping("/catalogo")
    public String catalogo() {
        return "redirect:/plantas";
    }
}