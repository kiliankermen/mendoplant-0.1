package com.mendoplant.app.controller;

import com.mendoplant.app.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Blog público de cuidado de plantas.
 */
@Controller
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("posts", blogService.listarPublicados());
        return "blog/list";
    }

    @GetMapping("/{slug}")
    public String detalle(@PathVariable String slug, Model model) {
        model.addAttribute("post", blogService.buscarPorSlug(slug));
        return "blog/detail";
    }

    @GetMapping("/guias-cuidado")
    public String guias(Model model) {
        model.addAttribute("guias", blogService.listarGuiasActivas());
        return "blog/guias";
    }
}
