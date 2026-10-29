package com.mendoplant.app.controller;

import com.mendoplant.app.security.UserPrincipal;
import com.mendoplant.app.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public String listar(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("plantas", wishlistService.listar(principal.getId()));
        return "wishlist/list";
    }

    @PostMapping("/agregar/{plantId}")
    public String agregar(@PathVariable Long plantId, @AuthenticationPrincipal UserPrincipal principal) {
        wishlistService.agregar(principal.getId(), plantId);
        return "redirect:/plantas/" + plantId;
    }

    @PostMapping("/quitar/{plantId}")
    public String quitar(@PathVariable Long plantId, @AuthenticationPrincipal UserPrincipal principal) {
        wishlistService.quitar(principal.getId(), plantId);
        return "redirect:/wishlist";
    }

    /** Alterna el favorito vía AJAX (sin recargar la página) y devuelve el nuevo estado. */
    @PostMapping("/toggle/{plantId}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long plantId, @AuthenticationPrincipal UserPrincipal principal) {
        boolean esta = wishlistService.contiene(principal.getId(), plantId);
        if (esta) {
            wishlistService.quitar(principal.getId(), plantId);
        } else {
            wishlistService.agregar(principal.getId(), plantId);
        }
        return Map.of("favorito", !esta);
    }
}
