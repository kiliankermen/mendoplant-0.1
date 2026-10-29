package com.mendoplant.app.controller;

import com.mendoplant.app.dto.ChangePasswordDTO;
import com.mendoplant.app.dto.UserRegistrationDTO;
import com.mendoplant.app.model.User;
import com.mendoplant.app.security.UserPrincipal;
import com.mendoplant.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Registro, login (vista) y gestión básica de cuenta (cambio de contraseña).
 * El procesamiento del login en sí lo maneja Spring Security (ver SecurityConfig).
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("usuario", new UserRegistrationDTO());
        return "auth/register";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("usuario") UserRegistrationDTO dto,
                            BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            dto.setEmail(dto.getEmail().trim().toLowerCase());
            userService.registrar(dto);
            redirectAttributes.addFlashAttribute("registrado", true);
            return "redirect:/login";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/cuenta")
    public String cuenta(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        User user = userService.buscarPorId(principal.getId());
        model.addAttribute("usuario", user);
        model.addAttribute("cambioPassword", new ChangePasswordDTO());
        return "auth/cuenta";
    }

    @PostMapping("/cuenta/nombre")
    public String cambiarNombre(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestParam String nombre, Model model) {
        User user = userService.buscarPorId(principal.getId());
        try {
            userService.cambiarNombre(principal.getId(), nombre);
            model.addAttribute("exitoNombre", "Nombre de usuario actualizado");
        } catch (RuntimeException ex) {
            model.addAttribute("errorNombre", ex.getMessage());
        }
        model.addAttribute("usuario", userService.buscarPorId(principal.getId()));
        model.addAttribute("cambioPassword", new ChangePasswordDTO());
        return "auth/cuenta";
    }

    @PostMapping("/cuenta/avatar")
    public String cambiarAvatar(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestParam("avatarFile") MultipartFile avatarFile, Model model) {
        User user = userService.buscarPorId(principal.getId());
        try {
            userService.cambiarAvatar(principal.getId(), avatarFile);
            model.addAttribute("exitoAvatar", "Foto de perfil actualizada");
        } catch (RuntimeException ex) {
            model.addAttribute("errorAvatar", ex.getMessage());
        }
        user = userService.buscarPorId(principal.getId());
        model.addAttribute("usuario", user);
        // El @ModelAttribute global calcula el avatar antes de este handler: lo forzamos acá
        // para que la respuesta refleje al instante la foto recién subida.
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            model.addAttribute("avatarUrl", user.getAvatarUrl());
        }
        model.addAttribute("cambioPassword", new ChangePasswordDTO());
        return "auth/cuenta";
    }

    @PostMapping("/cuenta/password")
    public String cambiarPassword(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @ModelAttribute("cambioPassword") ChangePasswordDTO dto,
                                   BindingResult result, Model model) {
        User user = userService.buscarPorId(principal.getId());
        if (result.hasErrors()) {
            model.addAttribute("usuario", user);
            return "auth/cuenta";
        }
        try {
            userService.cambiarPassword(principal.getId(), dto);
            model.addAttribute("exito", "Contraseña actualizada correctamente");
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("usuario", user);
        model.addAttribute("cambioPassword", new ChangePasswordDTO());
        return "auth/cuenta";
    }
}
