package com.mendoplant.app.controller;

import com.mendoplant.app.dto.PlantFilterDTO;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.*;
import com.mendoplant.app.service.CategoryService;
import com.mendoplant.app.service.EtiquetasUtil;
import com.mendoplant.app.service.FiltrosResumenUtil;
import com.mendoplant.app.service.GruposMultiplesBuilder;
import com.mendoplant.app.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.text.Collator;

/**
 * Subrutas de la tienda: cada categoría de producto tiene su propia URL
 * (/tienda/plantas, /tienda/macetas, ...) con su buscador y sus filtros.
 */
@Controller
@RequestMapping("/tienda")
@RequiredArgsConstructor
public class TiendaController {

    /** Cada página muestra 20 tarjetas. */
    private static final int TAMANO_PAGINA = 20;

    /** Una opción de filtro rápido (botón toggle). */
    private record OpcionToggle(String valor, String etiqueta, boolean activado) {}

    /** Un grupo de opciones del mismo campo, con su título. */
    private record GrupoToggle(String campo, String titulo, List<OpcionToggle> opciones) {}

    /** Una página de la numeración (numero <= 0 = puntos suspensivos). */
    private record PaginaLink(int numero, String url, boolean actual) {}

    /** Datos de la paginación: visible = hay más de 20 tarjetas. */
    private record PaginacionDTO(boolean visible, int paginaActual, int totalPaginas,
                                 String anterior, String siguiente, List<PaginaLink> paginas) {}

    private final PlantService plantService;
    private final CategoryService categoryService;
    private final FiltrosResumenUtil filtrosResumenUtil;
    private final EtiquetasUtil etiquetasUtil;
    private final GruposMultiplesBuilder gruposMultiplesBuilder;

    @GetMapping
    public String tienda(@ModelAttribute("filtro") PlantFilterDTO filtro,
                         @RequestParam(name = "page", defaultValue = "1") int page,
                         Model model) {
        List<Plant> todas = plantService.buscarCatalogoOrdenado(filtro, filtro.getOrden());
        model.addAttribute("plantas", paginar(todas, page));
        model.addAttribute("totalProductos", todas.size());
        model.addAttribute("paginacion", armarPaginacion("/tienda", todas, page, filtro));
        model.addAttribute("orden", filtro.getOrden());
        cargarCombosFiltros(model, filtro);
        model.addAttribute("filtro", filtro);
        boolean activo = tieneFiltrosTienda(filtro);
        model.addAttribute("filtroActivo", activo);
        if (activo) {
            model.addAttribute("filtrosResumen", filtrosResumenUtil.resumen(filtro));
        }
        model.addAttribute("modo", "todos");
        return "catalogo";
    }

    @GetMapping("/{categoria}")
    public String categoria(@PathVariable String categoria,
                            @ModelAttribute("filtro") PlantFilterDTO filtro,
                            @RequestParam(name = "page", defaultValue = "1") int page,
                            Model model) {
        Category cat = resolverCategoria(categoria);
        filtro.setCategoriaId(cat.getId());

        String slug = cat.getNombre().toLowerCase();
        model.addAttribute("categoriaSlug", slug);
        cargarResultado(model, "/tienda/" + slug, cat.getNombre(), filtro, page);

        model.addAttribute("gruposFiltro", gruposMultiplesBuilder.gruposFiltro(filtro));
        model.addAttribute("modo", modoDe(slug));

        // Filtros rápidos tipo toggle según la categoría
        model.addAttribute("filtrosToggle", armarFiltrosToggle(slug, filtro));

        return "tienda/categoria";
    }

    /** Solo el bloque de resultados (para refrescar la grilla vía AJAX sin recargar). */
    @GetMapping(path = "/{categoria}/resultado", produces = "text/html")
    public String categoriaResultado(@PathVariable String categoria,
                                     @ModelAttribute("filtro") PlantFilterDTO filtro,
                                     @RequestParam(name = "page", defaultValue = "1") int page,
                                     Model model) {
        Category cat = resolverCategoria(categoria);
        filtro.setCategoriaId(cat.getId());

        String slug = cat.getNombre().toLowerCase();
        cargarResultado(model, "/tienda/" + slug, cat.getNombre(), filtro, page);

        return "fragments/layout :: resultado";
    }

    private Category resolverCategoria(String categoria) {
        return categoryService.listarActivas().stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(categoria))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoria));
    }

    /**
     * Carga en el modelo los datos del bloque de resultados (grilla + paginación +
     * contadores + resumen de filtros). Compartido por la vista completa y por el
     * endpoint AJAX que refresca solo el fragmento.
     */
    private void cargarResultado(Model model, String basePath, String titulo,
                                 PlantFilterDTO filtro, int page) {
        boolean filtroActivo = tieneFiltrosActivos(filtro);
        List<Plant> todas = plantService.buscarCatalogoOrdenado(filtro, filtro.getOrden());
        model.addAttribute("categoriaTitulo", titulo);
        model.addAttribute("plantas", paginar(todas, page));
        model.addAttribute("totalProductos", todas.size());
        model.addAttribute("paginacion", armarPaginacion(basePath, todas, page, filtro));
        model.addAttribute("orden", filtro.getOrden());
        model.addAttribute("filtro", filtro);
        model.addAttribute("filtroActivo", filtroActivo);
        model.addAttribute("basePath", basePath);
        if (filtroActivo) {
            model.addAttribute("filtrosResumen", filtrosResumenUtil.resumen(filtro));
        }
    }

    /** Devuelve la porción de la lista correspondiente a la página pedida. */
    private List<Plant> paginar(List<Plant> todas, int page) {
        if (todas.isEmpty()) {
            return List.of();
        }
        int totalPaginas = (int) Math.ceil(todas.size() / (double) TAMANO_PAGINA);
        int paginaActual = Math.min(Math.max(1, page), totalPaginas);
        int desde = (paginaActual - 1) * TAMANO_PAGINA;
        int hasta = Math.min(desde + TAMANO_PAGINA, todas.size());
        return todas.subList(desde, hasta);
    }

    /** Construye el DTO de paginación preservando los filtros actuales en los links. */
    private PaginacionDTO armarPaginacion(String basePath, List<Plant> todas, int page, PlantFilterDTO filtro) {
        int totalPaginas = (int) Math.ceil(todas.size() / (double) TAMANO_PAGINA);
        int paginaActual = Math.min(Math.max(1, page), Math.max(1, totalPaginas));
        if (totalPaginas <= 1) {
            return new PaginacionDTO(false, paginaActual, 1, null, null, List.of());
        }
        List<PaginaLink> paginas = new ArrayList<>();
        int inicio = Math.max(1, paginaActual - 2);
        int fin = Math.min(totalPaginas, inicio + 4);
        inicio = Math.max(1, fin - 4);
        if (inicio > 1) {
            paginas.add(new PaginaLink(1, urlPagina(basePath, filtro, 1), paginaActual == 1));
            if (inicio > 2) {
                paginas.add(new PaginaLink(0, null, false));
            }
        }
        for (int p = inicio; p <= fin; p++) {
            paginas.add(new PaginaLink(p, urlPagina(basePath, filtro, p), p == paginaActual));
        }
        if (fin < totalPaginas) {
            if (fin < totalPaginas - 1) {
                paginas.add(new PaginaLink(0, null, false));
            }
            paginas.add(new PaginaLink(totalPaginas, urlPagina(basePath, filtro, totalPaginas), paginaActual == totalPaginas));
        }
        String anterior = paginaActual > 1 ? urlPagina(basePath, filtro, paginaActual - 1) : null;
        String siguiente = paginaActual < totalPaginas ? urlPagina(basePath, filtro, paginaActual + 1) : null;
        return new PaginacionDTO(true, paginaActual, totalPaginas, anterior, siguiente, paginas);
    }

    /** URL de una página conservando los filtros que haya activos (sin category en la ruta). */
    private String urlPagina(String basePath, PlantFilterDTO filtro, int pagina) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath(basePath);
        if (filtro != null) {
            if (filtro.getCategoriaId() != null) {
                b.queryParam("categoriaId", filtro.getCategoriaId());
            }
            agregarParam(b, "ambientes", filtro.getAmbientes());
            agregarParam(b, "climas", filtro.getClimas());
            agregarParam(b, "nivelesCuidado", filtro.getNivelesCuidado());
            agregarParam(b, "tiposPlanta", filtro.getTiposPlanta());
            agregarParam(b, "luces", filtro.getLuces());
            agregarParam(b, "tamanos", filtro.getTamanos());
            agregarParam(b, "tiposSustrato", filtro.getTiposSustrato());
            agregarParam(b, "tiposMaceta", filtro.getTiposMaceta());
            agregarParam(b, "tiposAccesorio", filtro.getTiposAccesorio());
            if (filtro.getPrecioMin() != null) {
                b.queryParam("precioMin", filtro.getPrecioMin());
            }
            if (filtro.getPrecioMax() != null) {
                b.queryParam("precioMax", filtro.getPrecioMax());
            }
            if (filtro.getTexto() != null && !filtro.getTexto().isBlank()) {
                b.queryParam("texto", filtro.getTexto());
            }
            if (filtro.getOrden() != null && !filtro.getOrden().isBlank()) {
                b.queryParam("orden", filtro.getOrden());
            }
            if (filtro.getModoFiltro() != null && !filtro.getModoFiltro().isBlank()) {
                b.queryParam("modoFiltro", filtro.getModoFiltro());
            }
        }
        b.queryParam("page", pagina);
        return b.build().encode().toUriString();
    }

    /** Agrega un parámetro multivalor: ?campo=A&campo=B */
    private void agregarParam(UriComponentsBuilder b, String nombre, Set<?> valores) {
        if (valores == null || valores.isEmpty()) {
            return;
        }
        for (Object valor : valores) {
            if (valor != null) {
                b.queryParam(nombre, valor);
            }
        }
    }

    private boolean tieneFiltrosActivos(PlantFilterDTO filtro) {
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

    private String nombreMaterial(TipoMaceta material) {
        return etiquetasUtil.tipoMaceta(material).replaceFirst("(?i)^maceta de ", "");
    }

    private <E extends Enum<E>> GrupoToggle grupoToggle(String campo, String titulo, E[] valores,
                                                       Function<E, String> etiqueta, Set<E> actuales) {
        List<OpcionToggle> opciones = new ArrayList<>();
        for (E v : valores) {
            boolean activo = actuales != null && actuales.contains(v);
            opciones.add(new OpcionToggle(v.name(), etiqueta.apply(v), activo));
        }
        return new GrupoToggle(campo, titulo, opciones);
    }

    private List<GrupoToggle> armarFiltrosToggle(String slug, PlantFilterDTO filtro) {
        return switch (slug) {
            case "macetas" -> List.of(
                    grupoToggle("tiposMaceta", "Material", TipoMaceta.values(), this::nombreMaterial, filtro.getTiposMaceta()),
                    grupoToggle("tamanos", "Tamaño", Tamano.values(), etiquetasUtil::tamano, filtro.getTamanos()));
            case "soportes" -> List.of(
                    grupoToggle("tiposAccesorio", "Tipo de accesorio", TipoAccesorio.values(), etiquetasUtil::tipoAccesorio, filtro.getTiposAccesorio()),
                    grupoToggle("tamanos", "Tamaño", Tamano.values(), etiquetasUtil::tamano, filtro.getTamanos()));
            case "sustratos" -> List.of(
                    grupoToggle("tiposSustrato", "Tipo de sustrato", TipoSustrato.values(), etiquetasUtil::tipoSustrato, filtro.getTiposSustrato()),
                    grupoToggle("tamanos", "Tamaño", Tamano.values(), etiquetasUtil::tamano, filtro.getTamanos()));
            default -> List.of(
                    grupoToggle("ambientes", "Ambiente", Ambiente.values(), etiquetasUtil::ambiente, filtro.getAmbientes()),
                    grupoToggle("nivelesCuidado", "Cuidado", NivelCuidado.values(), etiquetasUtil::nivelCuidado, filtro.getNivelesCuidado()),
                    grupoToggle("tamanos", "Tamaño", Tamano.values(), etiquetasUtil::tamano, filtro.getTamanos()),
                    grupoToggle("luces", "Luz", TipoLuz.values(), etiquetasUtil::luz, filtro.getLuces()),
                    grupoToggle("climas", "Clima", TipoClima.values(), etiquetasUtil::clima, filtro.getClimas()),
                    ordenarTipoPlanta(grupoToggle("tiposPlanta", "Tipo de planta",
                            Arrays.stream(TipoPlanta.values())
                                    .filter(tp -> tp != TipoPlanta.TROPICAL)
                                    .toArray(TipoPlanta[]::new),
                            etiquetasUtil::tipoPlanta, filtro.getTiposPlanta())));
        };
    }

    /** Ordena las opciones de "Tipo de planta" alfabéticamente (Árbol ≈ Arbol). */
    private GrupoToggle ordenarTipoPlanta(GrupoToggle grupo) {
        Collator col = Collator.getInstance(new Locale("es", "AR"));
        col.setStrength(Collator.SECONDARY);
        return new GrupoToggle(grupo.campo(), grupo.titulo(),
                grupo.opciones().stream()
                        .sorted((a, b) -> col.compare(a.etiqueta(), b.etiqueta()))
                        .toList());
    }

    private String modoDe(String slug) {
        return switch (slug) {
            case "macetas", "soportes", "sustratos" -> slug;
            default -> "plantas";
        };
    }

    /** ¿Hay filtros activos en el catálogo de /tienda? (incluye la categoría). */
    private boolean tieneFiltrosTienda(PlantFilterDTO filtro) {
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

    private void cargarCombosFiltros(Model model, PlantFilterDTO filtro) {
        model.addAttribute("categorias", categoryService.listarActivas());
        model.addAttribute("gruposFiltro", gruposMultiplesBuilder.gruposFiltro(filtro));
    }
}