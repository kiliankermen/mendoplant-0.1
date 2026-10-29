package com.mendoplant.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Responde el archivo "well-known" que Chrome DevTools consulta en segundo plano
 * ({@code /.well-known/appspecific/com.chrome.devtools.json}). Al estar permitido en
 * {@code SecurityConfig}, esa consulta no se guarda como "saved request" y no desvía
 * el redirect posterior al login hacia una URL inexistente. Además, al mapearse aquí,
 * recibe un 200 JSON en lugar de terminar en un 500.
 */
@RestController
public class WellKnownController {

    @GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
    public Map<String, Object> chromeDevTools() {
        return Map.of();
    }
}