package com.mendoplant.app.controller;

import com.mendoplant.app.security.UserPrincipal;
import com.mendoplant.app.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping("/leer")
    @ResponseBody
    public Map<String, Object> marcarLeidas(@AuthenticationPrincipal UserPrincipal principal) {
        notificacionService.marcarTodasComoLeidas(principal.getId());
        return Map.of("ok", true, "noLeidas", 0);
    }

    @PostMapping("/silenciar")
    public String alternarSilencio(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestParam(required = false) String redirect) {
        notificacionService.alternarSilencio(principal.getId());
        return redirigir(redirect);
    }

    @PostMapping("/eliminar-todas")
    public String eliminarTodas(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestParam(required = false) String redirect) {
        notificacionService.eliminarTodas(principal.getId());
        return redirigir(redirect);
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id,
                           @AuthenticationPrincipal UserPrincipal principal,
                           @RequestParam(required = false) String redirect) {
        notificacionService.eliminar(principal.getId(), id);
        return redirigir(redirect);
    }

    private String redirigir(String destino) {
        if (destino != null && destino.startsWith("/") && !destino.startsWith("//")
                && !destino.contains("\r") && !destino.contains("\n")) {
            return "redirect:" + destino;
        }
        return "redirect:/";
    }
}
