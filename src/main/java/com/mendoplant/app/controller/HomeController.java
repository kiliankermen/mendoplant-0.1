package com.mendoplant.app.controller;

import com.mendoplant.app.dto.PlantFilterDTO;
import com.mendoplant.app.dto.SeccionCatalogoDTO;
import com.mendoplant.app.model.*;
import com.mendoplant.app.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Portada / tienda principal: buscador con filtros, categorías, promociones,
 * ofertas del día, carruseles por sección y combos.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PlantService plantService;
    private final CategoryService categoryService;
    private final PromocionService promocionService;
    private final SiteConfigService siteConfigService;
    private final FiltrosResumenUtil filtrosResumenUtil;
    private final GruposMultiplesBuilder gruposMultiplesBuilder;

    /** Slugs de promoción estáticos soportados como filtro (la "primavera" ahora es una promo de BD). */
    private static final Map<String, String> PROMOS_SOPORTADAS = Map.of(
            "combos", "Combos que arman jardín",
            "oferta-dia", "Oferta del día"
    );

    private boolean esPromoValida(String promo) {
        if (promo == null) {
            return false;
        }
        return PROMOS_SOPORTADAS.containsKey(promo) || promo.startsWith("promocion-");
    }

    @GetMapping("/")
    public String home(@ModelAttribute("filtro") PlantFilterDTO filtro,
                       @RequestParam(value = "seccion", required = false) String seccion,
                       @RequestParam(value = "promo", required = false) String promo,
                       Model model) {

        aplicarSeccion(seccion, filtro);

        boolean buscadorActivo = tieneFiltrosBuscador(filtro);
        boolean promoActivo = esPromoValida(promo);

        // La portada muestra por defecto la categoría "Plantas" SOLO cuando no hay
        // ninguna búsqueda/filtro ni promo activa. Si el usuario busca por texto
        // (p. ej. "Palmera"), no se restringe a una categoría: se ve todo, incluidas
        // las plantas de otras categorías (Interior, etc.).
        if (filtro.getCategoriaId() == null && !buscadorActivo && !promoActivo) {
            categoryService.listarActivas().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase("Plantas"))
                    .findFirst()
                    .ifPresent(c -> filtro.setCategoriaId(c.getId()));
        }

        List<Plant> plantas = plantService.buscarCatalogoOrdenado(filtro, filtro.getOrden());
        if (promoActivo) {
            plantas = filtradasPorPromo(plantas, promo);
        }

        boolean filtroActivo = buscadorActivo || promoActivo;

        model.addAttribute("plantas", plantas);
        model.addAttribute("totalProductos", plantas.size());
        model.addAttribute("seccionActiva", seccion != null ? seccion : "plantas");
        model.addAttribute("filtro", filtro);
        model.addAttribute("orden", filtro.getOrden());
        model.addAttribute("filtroActivo", filtroActivo);
        model.addAttribute("promoActivo", promoActivo);
        model.addAttribute("promoParam", promo);
        if (filtroActivo) {
            model.addAttribute("filtrosResumen", construirResumen(filtro, promo, promoActivo));
        }

        // Combos de los filtros
        model.addAttribute("categorias", categoryService.listarActivas());
        model.addAttribute("gruposFiltro", gruposMultiplesBuilder.gruposFiltro(filtro));

        // Carrusel de promociones
        model.addAttribute("promos", promocionService.promocionesActivas());

        // Ofertas del día
        List<Plant> ofertasDia = promocionService.ofertasDelDia();
        model.addAttribute("ofertasDia", ofertasDia);
        model.addAttribute("ofertaRebaja", ofertasDia.isEmpty() ? null : ofertasDia.get(0));
        List<Plant> combos = plantService.buscarCombos();
        model.addAttribute("ofertaCombo", combos.isEmpty() ? null : combos.get(0));
        model.addAttribute("combos", combos);

        // Secciones de la portada
        model.addAttribute("secciones", armarSecciones());

        return "home";
    }

    @GetMapping("/terminos")
    public String terminos(Model model) {
        model.addAttribute("terminos", siteConfigService.obtener("TERMS_CONDITIONS", ""));
        return "terminos";
    }

    @GetMapping("/error/403")
    public String error403() {
        return "error/403";
    }

    private void aplicarSeccion(String seccion, PlantFilterDTO filtro) {
        if (seccion == null || "plantas".equals(seccion)) {
            return;
        }
        for (Category cat : categoryService.listarActivas()) {
            if (cat.getNombre().equalsIgnoreCase(seccion)) {
                filtro.setCategoriaId(cat.getId());
                return;
            }
        }
    }

    private List<SeccionCatalogoDTO> armarSecciones() {
        List<SeccionCatalogoDTO> secciones = new ArrayList<>();
        secciones.add(new SeccionCatalogoDTO("Plantas de interior", plantService.buscarPorAmbiente(Ambiente.INTERIOR)));
        secciones.add(new SeccionCatalogoDTO("Plantas de exterior", plantService.buscarPorAmbiente(Ambiente.EXTERIOR)));

        TipoPlanta[] orden = {
                TipoPlanta.CACTUS, TipoPlanta.SUCULENTA, TipoPlanta.HELECHO,
                TipoPlanta.FLOR, TipoPlanta.ARBOL, TipoPlanta.ENREDADERA,
                TipoPlanta.PALMERA, TipoPlanta.ORQUIDEA, TipoPlanta.AROMATICA,
                TipoPlanta.TREBOL, TipoPlanta.ARBUSTO, TipoPlanta.TROPICAL
        };
        for (TipoPlanta tipo : orden) {
            List<Plant> plantas = plantService.buscarPorTipoPlanta(tipo);
            if (!plantas.isEmpty()) {
                secciones.add(new SeccionCatalogoDTO(detalleTexto(tipo), plantas));
            }
        }
        return secciones;
    }

    private boolean tieneFiltrosBuscador(PlantFilterDTO filtro) {
        return filtro != null && (
                tieneValores(filtro.getAmbientes())
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

    private List<Plant> filtradasPorPromo(List<Plant> plantas, String slug) {
        return promocionService.filtrarPorPromo(slug, plantas);
    }

    private String construirResumen(PlantFilterDTO filtro, String promo, boolean promoActivo) {
        List<String> partes = new ArrayList<>();
        if (promoActivo && promo != null) {
            partes.add(promocionService.tituloPromo(promo));
        }
        String detalle = filtrosResumenUtil.resumen(filtro);
        if (!"filtros aplicados".equals(detalle)) {
            partes.add(detalle);
        }
        return partes.isEmpty() ? "filtros aplicados" : String.join(", ", partes);
    }

    private String detalleTexto(TipoPlanta tipo) {
        return switch (tipo) {
            case SUCULENTA -> "Suculentas";
            case CACTUS -> "Cactus";
            case HELECHO -> "Helechos";
            case FLOR -> "Flores";
            case TREBOL -> "Tréboles";
            case ARBOL -> "Árboles";
            case ARBUSTO -> "Arbustos";
            case ENREDADERA -> "Enredaderas";
            case PALMERA -> "Palmeras";
            case ORQUIDEA -> "Orquídeas";
            case AROMATICA -> "Aromáticas y hierbas";
            case TROPICAL -> "Tropicales";
        };
    }
}