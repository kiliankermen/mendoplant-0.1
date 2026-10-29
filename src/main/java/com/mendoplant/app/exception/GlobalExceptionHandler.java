package com.mendoplant.app.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Manejo global de excepciones para toda la aplicación (vistas Thymeleaf).
 * Traduce las excepciones de negocio/dominio en páginas de error amigables,
 * evitando exponer stacktraces al usuario final.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
        ModelAndView mv = new ModelAndView("error/404");
        mv.addObject("mensaje", ex.getMessage());
        mv.setStatus(HttpStatus.NOT_FOUND);
        return mv;
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException ex, HttpServletRequest request, Model model) {
        log.warn("Error de negocio en {}: {}", request.getRequestURI(), ex.getMessage());
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("mensaje", ex.getMessage());
        mv.setStatus(HttpStatus.BAD_REQUEST);
        return mv;
    }

    /** Conflicto por optimistic locking: otro usuario guardó primero la misma entidad. */
    @ExceptionHandler({
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ModelAndView handleOptimisticLock(Exception ex, HttpServletRequest request) {
        log.warn("Conflicto de concurrencia (optimistic lock) en {}: {}", request.getRequestURI(), ex.getMessage());
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("mensaje",
                "El registro fue modificado por otra persona mientras lo editabas. "
                        + "Recargá la página y volvé a intentarlo.");
        mv.setStatus(HttpStatus.CONFLICT);
        return mv;
    }

    /** Datos que violan restricciones de la base (unicidad, FK, no nulos, ...). */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIntegrity(org.springframework.dao.DataIntegrityViolationException ex,
                                        HttpServletRequest request) {
        log.warn("Violación de integridad en {}: {}", request.getRequestURI(), ex.getMessage());
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("mensaje",
                "No se pudo guardar: los datos entran en conflicto con información existente "
                        + "(por ejemplo, un nombre o email ya registrado). Revisá y volvé a intentarlo.");
        mv.setStatus(HttpStatus.BAD_REQUEST);
        return mv;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        ModelAndView mv = new ModelAndView("error/403");
        mv.setStatus(HttpStatus.FORBIDDEN);
        return mv;
    }

    /** Ruta sin controller ni recurso estático (Spring 6.1+): 404 normal, no 500. */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex,
                                         HttpServletRequest request) {
        log.warn("Recurso no encontrado (estático) en {}: {}", request.getRequestURI(), ex.getMessage());
        ModelAndView mv = new ModelAndView("error/404");
        mv.setStatus(HttpStatus.NOT_FOUND);
        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("mensaje", "Ocurrió un error inesperado. Por favor intentá nuevamente.");
        mv.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mv;
    }
}
