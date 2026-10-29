package com.mendoplant.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Renderiza la página de acceso denegado (403) también cuando Spring Security reenvía
 * (forward) la petición original (p. ej. un POST rechazado por CSRF) hacia {@code /error/403},
 * evitando que el reenvío con método POST termine en un 500.
 */
@Controller
public class ErrorPagesController {

    @RequestMapping("/error/403")
    public ModelAndView forbidden() {
        ModelAndView mv = new ModelAndView("error/403");
        mv.setStatus(HttpStatus.FORBIDDEN);
        return mv;
    }
}