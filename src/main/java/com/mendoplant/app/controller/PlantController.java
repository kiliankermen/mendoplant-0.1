package com.mendoplant.app.controller;

import com.mendoplant.app.dto.PlantDTO;
import com.mendoplant.app.dto.PlantFilterDTO;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.*;
import com.mendoplant.app.security.UserPrincipal;
import com.mendoplant.app.service.CategoryService;
import com.mendoplant.app.service.FiltrosResumenUtil;
import com.mendoplant.app.service.GruposMultiplesBuilder;
import com.mendoplant.app.service.ImageStorageService;
import com.mendoplant.app.service.PlantService;
import com.mendoplant.app.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Catálogo público de plantas + publicación/edición por parte de vendedores autenticados.
 */
@Controller
@RequestMapping("/plantas")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;
    private final CategoryService categoryService;
    private final WishlistService wishlistService;
    private final ImageStorageService imageStorageService;
    private final FiltrosResumenUtil filtrosResumenUtil;
    private final GruposMultiplesBuilder gruposMultiplesBuilder;

    @GetMapping({"", "/"})
    public String listar(@ModelAttribute("filtro") PlantFilterDTO filtro, Model model,
                          @AuthenticationPrincipal UserPrincipal principal) {
        List<Plant> plantas = plantService.buscarCatalogoOrdenado(filtro, filtro.getOrden());
        model.addAttribute("plantas", plantas);
        model.addAttribute("totalProductos", plantas.size());
        cargarCombosFiltros(model, filtro);
        model.addAttribute("filtro", filtro);
        model.addAttribute("orden", filtro.getOrden());
        boolean activo = tieneFiltros(filtro);
        model.addAttribute("filtroActivo", activo);
        if (activo) {
            model.addAttribute("filtrosResumen", filtrosResumenUtil.resumen(filtro));
        }
        model.addAttribute("modo", "todos");
        // El catálogo completo vive en /plantas; /catalogo se mantiene por compatibilidad.
        return "catalogo";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Model model,
                           @AuthenticationPrincipal UserPrincipal principal) {
        if (id != null && id.matches("\\d+")) {
            long pid = Long.parseLong(id);
            Plant planta = plantService.buscarPorId(pid);
            String slug = slugNombreCientifico(planta);
            if (slug != null && !slug.isBlank()) {
                return "redirect:/plantas/" + slug;
            }
            model.addAttribute("planta", planta);
            if (principal != null) {
                model.addAttribute("enWishlist", wishlistService.contiene(principal.getId(), pid));
            }
            return "plants/detail";
        }
        String slug = normalizarSlug(id);
        Plant planta = plantService.buscarPorNombreCientificoSlug(slug);
        if (planta == null) {
            planta = plantService.buscarPorNombreCientifico(id);
        }
        if (planta == null) {
            throw new ResourceNotFoundException("Planta no encontrada: " + id);
        }
        model.addAttribute("planta", planta);
        if (principal != null) {
            model.addAttribute("enWishlist", wishlistService.contiene(principal.getId(), planta.getId()));
        }
        return "plants/detail";
    }

    private String normalizarSlug(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '-')
                .replace('_', '-');
    }

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        Plant plant = plantService.buscarPorId(id);
        PlantDTO dto = armarDtoDesdePlant(plant);
        model.addAttribute("plantaDto", dto);
        cargarCombosFormulario(model, plant);
        model.addAttribute("modoEdicion", true);
        return "plants/form";
    }

    @PostMapping("/editar/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("plantaDto") PlantDTO dto,
                              BindingResult result,
                              @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
                              @AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (result.hasErrors()) {
            cargarCombosFormulario(model, dto);
            model.addAttribute("modoEdicion", true);
            return "plants/form";
        }
        Plant previa = plantService.buscarPorId(id);
        if (imagenFile != null && !imagenFile.isEmpty()) {
            try {
                dto.setImagenUrl(imageStorageService.guardar(imagenFile));
                imageStorageService.eliminar(previa.getImagenUrl());
            } catch (IllegalArgumentException e) {
                model.addAttribute("imagenError", e.getMessage());
                cargarCombosFormulario(model, dto);
                model.addAttribute("modoEdicion", true);
                return "plants/form";
            }
        } else if (dto.getImagenUrl() == null || dto.getImagenUrl().isBlank()) {
            // Sin imagen nueva en la edición: se conserva la actual.
            dto.setImagenUrl(previa.getImagenUrl());
        }
        plantService.actualizar(id, dto, principal.getId());
        return "redirect:/plantas/" + id;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        plantService.eliminar(id, principal.getId());
        return "redirect:/dashboard/mis-combos";
    }

    @GetMapping("/mis-publicaciones")
    public String misPublicaciones() {
        return "redirect:/dashboard/mis-combos";
    }

    private boolean tieneFiltros(PlantFilterDTO filtro) {
        return filtro != null && (
                filtro.getCategoriaId() != null
                || tieneValores(filtro.getAmbientes())
                || tieneValores(filtro.getClimas())
                || tieneValores(filtro.getNivelesCuidado())
                || tieneValores(filtro.getTiposPlanta())
                || tieneValores(filtro.getLuces())
                || tieneValores(filtro.getTamanos())
                || tieneValores(filtro.getTiposSustrato())
                || tieneValores(filtro.getTiposMaceta())
                || tieneValores(filtro.getTiposAccesorio())
                || filtro.getPrecioMin() != null
                || filtro.getPrecioMax() != null
                || (filtro.getTexto() != null && !filtro.getTexto().isBlank()));
    }

    private boolean tieneValores(Set<?> valores) {
        return valores != null && !valores.isEmpty();
    }

    private void cargarCombosFiltros(Model model, PlantFilterDTO filtro) {
        model.addAttribute("categorias", categoryService.listarActivas());
        model.addAttribute("gruposFiltro", gruposMultiplesBuilder.gruposFiltro(filtro));
    }

    private void cargarCombosFormulario(Model model, PlantDTO dto) {
        model.addAttribute("categorias", categoryService.listarActivas());
        model.addAttribute("gruposFormulario", gruposMultiplesBuilder.gruposFormulario(dto));
    }

    private String slugNombreCientifico(Plant planta) {
        if (planta == null || planta.getNombreCientifico() == null) {
            return null;
        }
        String nombre = planta.getNombreCientifico().trim().toLowerCase(Locale.ROOT);
        return nombre.replace(' ', '-').replace('_', '-');
    }

    private void cargarCombosFormulario(Model model, Plant planta) {
        model.addAttribute("categorias", categoryService.listarActivas());
        model.addAttribute("gruposFormulario", gruposMultiplesBuilder.gruposFormulario(planta));
    }

    private PlantDTO armarDtoDesdePlant(Plant plant) {
        PlantDTO dto = new PlantDTO();
        dto.setId(plant.getId());
        dto.setNombreComun(plant.getNombreComun());
        dto.setNombreCientifico(plant.getNombreCientifico());
        dto.setDescripcion(plant.getDescripcion());
        dto.setPrecio(plant.getPrecio());
        dto.setStock(plant.getStock());
        dto.setImagenUrl(plant.getImagenUrl());
        dto.setCategoriaId(plant.getCategoria() != null ? plant.getCategoria().getId() : null);
        // Los atributos multivalor NO se copian al DTO: se marcan en el formulario
        // con el modelo "gruposFormulario" y llegan desde las casillas marcadas.
        dto.setEsCombo(plant.isEsCombo());
        return dto;
    }
}
