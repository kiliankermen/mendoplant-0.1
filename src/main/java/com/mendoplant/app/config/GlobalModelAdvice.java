package com.mendoplant.app.config;

import com.mendoplant.app.dto.CarritoItemDTO;
import com.mendoplant.app.dto.NotificacionDTO;
import com.mendoplant.app.model.User;
import com.mendoplant.app.service.CarritoService;
import com.mendoplant.app.service.NotificacionService;
import com.mendoplant.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Atributos globales disponibles en todas las vistas:
 * carrito de la sesión, bandeja de notificaciones, usuario en sesión.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CarritoService carritoService;
    private final NotificacionService notificacionService;
    private final UserService userService;



    private User usuarioEnSesion() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return null;
            }
            String email = auth.getName();
            if (email == null || email.isBlank()) {
                return null;
            }
            return userService.buscarPorEmail(email);
        } catch (RuntimeException e) {
            // Nunca debe tumbar una vista: sin usuario en sesión se renderiza el modo invitado.
            return null;
        }
    }

    @ModelAttribute("carritoItems")
    public List<CarritoItemDTO> carritoItems() {
        try {
            return carritoService.items();
        } catch (RuntimeException e) {
            return java.util.Collections.emptyList();
        }
    }

    @ModelAttribute("carritoCantidad")
    public int carritoCantidad() {
        try {
            return carritoService.cantidadItems();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    @ModelAttribute("carritoTotal")
    public BigDecimal carritoTotal() {
        try {
            return carritoService.total();
        } catch (RuntimeException e) {
            return BigDecimal.ZERO;
        }
    }

    @ModelAttribute("carritoVacio")
    public boolean carritoVacio() {
        try {
            return carritoService.vacio();
        } catch (RuntimeException e) {
            return true;
        }
    }

    @ModelAttribute("notificaciones")
    public List<NotificacionDTO> notificaciones() {
        try {
            User usuario = usuarioEnSesion();
            return notificacionService.notificacionesPara(usuario != null ? usuario.getId() : null);
        } catch (RuntimeException e) {
            return java.util.Collections.emptyList();
        }
    }

    @ModelAttribute("notificacionesNoLeidas")
    public long notificacionesNoLeidas() {
        try {
            User usuario = usuarioEnSesion();
            return notificacionService.noLeidas(usuario != null ? usuario.getId() : null);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    @ModelAttribute("notificacionesSilenciadas")
    public boolean notificacionesSilenciadas() {
        try {
            User usuario = usuarioEnSesion();
            return usuario != null && usuario.isNotificacionesSilenciadas();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Expone el token CSRF para poder marcar notificaciones como leídas vía AJAX. */
    @ModelAttribute
    public void csrf(HttpServletRequest request, Model model) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            model.addAttribute("_csrf", token);
        }
    }

    /** Ruta actual (requestURI) para marcar la sección activa en la navegación. */
    @ModelAttribute("urlActual")
    public String urlActual(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("misFavoritos")
    public Set<Long> misFavoritos() {
        User usuario = usuarioEnSesion();
        if (usuario == null) {
            return new HashSet<>();
        }
        try {
            return usuario.getListaDeseos().stream()
                    .map(p -> p.getId())
                    .collect(Collectors.toSet());
        } catch (RuntimeException e) {
            // La wishlist puede inicializarse de forma perezosa; ante cualquier fallo se muestra sin favoritos.
            return new HashSet<>();
        }
    }

    @ModelAttribute("avatarUrl")
    public String avatarUrl() {
        try {
            User usuario = usuarioEnSesion();
            if (usuario == null) {
                return "";
            }
            // Si el usuario subió una foto de perfil, se muestra esa; si no, un avatar
            // SVG generado con la inicial del nombre.
            if (usuario.getAvatarUrl() != null && !usuario.getAvatarUrl().isBlank()) {
                return usuario.getAvatarUrl();
            }
            return avatarDataUri(usuario.getNombre());
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String avatarDataUri(String nombre) {
        String inicial = primeraInicial(nombre);
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='128' height='128'>"
                + "<rect width='128' height='128' fill='#2f6b3a'/>"
                + "<text x='50%' y='52%' text-anchor='middle' dy='.35em'"
                + " font-family='Arial, sans-serif' font-size='46' font-weight='bold' fill='#ffffff'>"
                + inicial + "</text></svg>";
        return "data:image/svg+xml;charset=utf-8," + java.net.URLEncoder.encode(svg, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String primeraInicial(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "M";
        }
        return String.valueOf(nombre.trim().charAt(0)).toUpperCase();
    }
}