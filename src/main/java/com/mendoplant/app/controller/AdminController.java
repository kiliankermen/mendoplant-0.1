package com.mendoplant.app.controller;

import com.mendoplant.app.dto.BlogPostDTO;
import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.model.*;
import com.mendoplant.app.security.UserPrincipal;
import com.mendoplant.app.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Panel de administración: categorías, tipos de sustrato/maceta (vía enums, ver nota abajo),
 * gestión de usuarios (roles / activación), blog y contenido del sitio (Home, Términos).
 *
 * Nota: TipoMaceta y TipoSustrato se modelan como Enum (según lo solicitado como alternativa
 * en la consigna) por simplicidad y consistencia de datos; por eso no llevan CRUD propio como
 * Category, que sí es una entidad administrable en base de datos.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CategoryService categoryService;
    private final UserService userService;
    private final BlogService blogService;
    private final SiteConfigService siteConfigService;
    private final PlantService plantService;
    private final OrderService orderService;
    private final EnvioService envioService;
    private final FacturaService facturaService;
    private final PagoService pagoService;
    private final PromocionService promocionService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsuarios", userService.listarTodos().size());
        model.addAttribute("totalCategorias", categoryService.listarTodas().size());
        model.addAttribute("totalPosts", blogService.listarTodos().size());
        model.addAttribute("totalPedidos", orderService.listarTodas().size());
        model.addAttribute("pedidosPendientes", orderService.contarPorEstado(EstadoOrden.PENDIENTE));
        model.addAttribute("pedidosCompletados", orderService.contarPorEstado(EstadoOrden.COMPLETADO));
        model.addAttribute("totalFacturas", facturaService.listarTodas().size());
        model.addAttribute("totalPromos", promocionService.listarPromociones().size());
        return "admin/dashboard";
    }

    // ---------------------------------------------------------------
    // Categorías
    // ---------------------------------------------------------------
    @GetMapping("/categorias")
    public String categorias(Model model) {
        model.addAttribute("categorias", categoryService.listarTodas());
        model.addAttribute("categoria", new Category());
        return "admin/categorias";
    }

    @PostMapping("/categorias")
    public String guardarCategoria(@ModelAttribute Category categoria) {
        categoryService.guardar(categoria);
        return "redirect:/admin/categorias";
    }

    @GetMapping("/categorias/editar/{id}")
    public String editarCategoria(@PathVariable Long id, Model model) {
        model.addAttribute("categorias", categoryService.listarTodas());
        model.addAttribute("categoria", categoryService.buscarPorId(id));
        return "admin/categorias";
    }

    @PostMapping("/categorias/eliminar/{id}")
    public String eliminarCategoria(@PathVariable Long id) {
        categoryService.eliminar(id);
        return "redirect:/admin/categorias";
    }

    // ---------------------------------------------------------------
    // Categorías de portada de las publicaciones (solo el admin elige
    // qué plantas destacan en la página principal asignándoles categoría).
    // ---------------------------------------------------------------
    @GetMapping("/plantas")
    public String plantas(Model model) {
        model.addAttribute("plantas", plantService.buscarConFiltros(null));
        model.addAttribute("categorias", categoryService.listarTodas());
        return "admin/plantas";
    }

    @PostMapping("/plantas/{id}/categoria")
    public String asignarCategoria(@PathVariable Long id,
                                   @RequestParam(required = false) Long categoriaId) {
        plantService.cambiarCategoria(id, categoriaId);
        return "redirect:/admin/plantas";
    }

    @PostMapping("/plantas/{id}/eliminar")
    public String eliminarPlanta(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        plantService.eliminar(id, principal.getId());
        return "redirect:/admin/plantas";
    }

    // ---------------------------------------------------------------
    // Usuarios
    // ---------------------------------------------------------------
    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", userService.listarTodos());
        model.addAttribute("roles", Role.values());
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRol(@PathVariable Long id, @RequestParam Role rol) {
        userService.cambiarRol(id, rol);
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        userService.cambiarEstado(id, activo);
        return "redirect:/admin/usuarios";
    }

    // ---------------------------------------------------------------
    // Blog
    // ---------------------------------------------------------------
    @GetMapping("/blog")
    public String blog(Model model) {
        model.addAttribute("posts", blogService.listarTodos());
        return "admin/blog";
    }

    @GetMapping("/blog/nueva")
    public String nuevoPostForm(Model model) {
        model.addAttribute("postDto", new BlogPostDTO());
        model.addAttribute("modoEdicion", false);
        return "admin/blog-form";
    }

    @PostMapping("/blog/nueva")
    public String crearPost(@Valid @ModelAttribute("postDto") BlogPostDTO dto, BindingResult result,
                             Principal principal, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("modoEdicion", false);
            return "admin/blog-form";
        }
        Long autorId = userService.buscarPorEmail(principal.getName()).getId();
        blogService.crear(dto, autorId);
        return "redirect:/admin/blog";
    }

    @GetMapping("/blog/editar/{id}")
    public String editarPostForm(@PathVariable Long id, Model model) {
        BlogPost post = blogService.buscarPorId(id);
        BlogPostDTO dto = new BlogPostDTO(post.getId(), post.getTitulo(), post.getContenido(), post.getCategoria());
        model.addAttribute("postDto", dto);
        model.addAttribute("modoEdicion", true);
        return "admin/blog-form";
    }

    @PostMapping("/blog/editar/{id}")
    public String actualizarPost(@PathVariable Long id, @Valid @ModelAttribute("postDto") BlogPostDTO dto,
                                  BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("modoEdicion", true);
            return "admin/blog-form";
        }
        blogService.actualizar(id, dto);
        return "redirect:/admin/blog";
    }

    @PostMapping("/blog/eliminar/{id}")
    public String eliminarPost(@PathVariable Long id) {
        blogService.eliminar(id);
        return "redirect:/admin/blog";
    }

    // ---------------------------------------------------------------
    // Guías de cuidado
    // ---------------------------------------------------------------
    @GetMapping("/guias")
    public String guias(Model model) {
        model.addAttribute("guias", blogService.listarGuias());
        model.addAttribute("guia", new CareGuide());
        model.addAttribute("plantas", plantService.destacadas());
        return "admin/guias";
    }

    @PostMapping("/guias")
    public String guardarGuia(@ModelAttribute CareGuide guia, @RequestParam(required = false) Long plantId) {
        if (plantId != null) {
            guia.setPlant(plantService.buscarPorId(plantId));
        } else {
            guia.setPlant(null);
        }
        blogService.guardarGuia(guia);
        return "redirect:/admin/guias";
    }

    @PostMapping("/guias/eliminar/{id}")
    public String eliminarGuia(@PathVariable Long id) {
        blogService.eliminarGuia(id);
        return "redirect:/admin/guias";
    }

    // ---------------------------------------------------------------
    // Contenido del sitio (Home, Términos y Condiciones)
    // ---------------------------------------------------------------
    @GetMapping("/contenido")
    public String contenido(Model model) {
        model.addAttribute("homeTitle", siteConfigService.obtener("HOME_TITLE", "Bienvenido a Mendo Plant"));
        model.addAttribute("homeSubtitle", siteConfigService.obtener("HOME_SUBTITLE", ""));
        model.addAttribute("homeBanner", siteConfigService.obtener("HOME_BANNER", ""));
        model.addAttribute("terminos", siteConfigService.obtener("TERMS_CONDITIONS", ""));
        return "admin/contenido";
    }

    @PostMapping("/contenido")
    public String guardarContenido(@RequestParam String homeTitle, @RequestParam String homeSubtitle,
                                    @RequestParam String homeBanner, @RequestParam String terminos) {
        siteConfigService.guardar("HOME_TITLE", homeTitle);
        siteConfigService.guardar("HOME_SUBTITLE", homeSubtitle);
        siteConfigService.guardar("HOME_BANNER", homeBanner);
        siteConfigService.guardar("TERMS_CONDITIONS", terminos);
        return "redirect:/admin/contenido";
    }

    // ---------------------------------------------------------------
    // Pedidos (órdenes de compra)
    // ---------------------------------------------------------------
    @GetMapping("/ordenes")
    public String ordenes(Model model) {
        model.addAttribute("ordenes", orderService.listarTodas());
        model.addAttribute("estados", EstadoOrden.values());
        return "admin/ordenes";
    }

    @GetMapping("/ordenes/{id}")
    public String detalleOrden(@PathVariable Long id, Model model) {
        model.addAttribute("orden", orderService.buscarPorId(id));
        model.addAttribute("envio", envioService.buscarPorOrden(id));
        model.addAttribute("factura", facturaService.buscarPorOrden(id));
        model.addAttribute("estados", EstadoOrden.values());
        model.addAttribute("estadosEnvio", EstadoEnvio.values());
        return "admin/orden-detalle";
    }

    /** Cambia el estado de la orden (solo PENDIENTE -> CANCELADO desde el admin). */
    @PostMapping("/ordenes/{id}/estado")
    public String cambiarEstadoOrden(@PathVariable Long id, @RequestParam EstadoOrden estado) {
        orderService.cambiarEstado(id, estado);
        return "redirect:/admin/ordenes/" + id;
    }

    /** Actualiza el envío de la orden: estado de despacho, transportista y tracking. */
    @PostMapping("/ordenes/{id}/envio")
    public String actualizarEnvio(@PathVariable Long id, @RequestParam EstadoEnvio estado,
                                  @RequestParam(required = false) String transportista,
                                  @RequestParam(required = false) String tracking) {
        Envio envio = envioService.buscarPorOrden(id);
        if (envio != null) {
            envioService.actualizarEstado(envio.getId(), estado, transportista, tracking);
        }
        return "redirect:/admin/ordenes/" + id;
    }

    /** Re-consulta el estado del pago en Mercado Pago y actualiza la orden local. */
    @PostMapping("/ordenes/{id}/verificar-pago")
    public String verificarPago(@PathVariable Long id) {
        pagoService.verificarYActualizar(id);
        return "redirect:/admin/ordenes/" + id;
    }

    /** Reintenta emitir la factura de una orden pagada (si no se generó automáticamente). */
    @PostMapping("/ordenes/{id}/facturar")
    public String facturar(@PathVariable Long id) {
        facturaService.emitirPara(id);
        return "redirect:/admin/ordenes/" + id;
    }

    @PostMapping("/facturas/{id}/anular")
    public String anularFactura(@PathVariable Long id) {
        Factura factura = facturaService.buscarPorId(id);
        facturaService.anular(id);
        return "redirect:/admin/ordenes/" + factura.getOrden().getId();
    }

    // ---------------------------------------------------------------
    // Promociones (descuentos por porcentaje)
    // ---------------------------------------------------------------
    @GetMapping("/promociones")
    public String promociones(Model model) {
        model.addAttribute("promos", promocionService.listarPromociones());
        model.addAttribute("promo", new Promocion());
        cargarCombosPromo(model);
        return "admin/promociones";
    }

    @GetMapping("/promociones/editar/{id}")
    public String editarPromocion(@PathVariable Long id, Model model) {
        model.addAttribute("promos", promocionService.listarPromociones());
        model.addAttribute("promo", promocionService.buscarPorId(id));
        cargarCombosPromo(model);
        return "admin/promociones";
    }

    @PostMapping("/promociones")
    public String guardarPromocion(@Valid @ModelAttribute("promo") Promocion promo, BindingResult result,
                                   @RequestParam(required = false) Long categoriaId,
                                   @RequestParam(required = false) List<Long> plantaIds,
                                   Model model) {
        if (result.hasErrors()) {
            model.addAttribute("promos", promocionService.listarPromociones());
            cargarCombosPromo(model);
            return "admin/promociones";
        }
        try {
            if (promo.getId() != null) {
                Promocion existente = promocionService.buscarPorId(promo.getId());
                existente.setNombre(promo.getNombre());
                existente.setPorcentaje(promo.getPorcentaje());
                existente.setDiasSemana(promo.getDiasSemana());
                existente.setFechaInicio(promo.getFechaInicio());
                existente.setFechaFin(promo.getFechaFin());
                existente.setTipoAplicacion(promo.getTipoAplicacion());
                existente.setClima(promo.getClima());
                existente.setActiva(promo.isActiva());
                existente.setCategoria(categoriaId != null ? categoryService.buscarPorId(categoriaId) : null);
                existente.setPlantas(resolverPlantas(plantaIds));
                promocionService.guardarPromocion(existente);
            } else {
                promo.setCategoria(categoriaId != null ? categoryService.buscarPorId(categoriaId) : null);
                promo.setPlantas(resolverPlantas(plantaIds));
                promocionService.guardarPromocion(promo);
            }
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("promos", promocionService.listarPromociones());
            cargarCombosPromo(model);
            return "admin/promociones";
        }
        return "redirect:/admin/promociones";
    }

    @PostMapping("/promociones/eliminar/{id}")
    public String desactivarPromocion(@PathVariable Long id) {
        promocionService.desactivarPromocion(id);
        return "redirect:/admin/promociones";
    }

    // ---------------------------------------------------------------
    // Oferta del día (planta destacada por día de la semana)
    // ---------------------------------------------------------------
    @GetMapping("/ofertas-dia")
    public String ofertasDia(Model model) {
        model.addAttribute("ofertas", promocionService.listarOfertasDia());
        model.addAttribute("diasSemana", diasSemana());
        model.addAttribute("plantas", plantService.buscarConFiltros(null));
        model.addAttribute("oferta", new OfertaDia());
        return "admin/ofertas-dia";
    }

    @PostMapping("/ofertas-dia")
    public String guardarOfertaDia(@RequestParam Integer diaSemana,
                                   @RequestParam Long plantaId,
                                   @RequestParam java.math.BigDecimal porcentaje,
                                   @RequestParam(defaultValue = "true") boolean activa) {
        promocionService.guardarOfertaDia(diaSemana, plantaId, porcentaje, activa);
        return "redirect:/admin/ofertas-dia";
    }

    private void cargarCombosPromo(Model model) {
        model.addAttribute("categorias", categoryService.listarActivas());
        model.addAttribute("plantas", plantService.buscarConFiltros(null));
        model.addAttribute("tiposAplicacion", TipoAplicacion.values());
        model.addAttribute("tiposClima", TipoClima.values());
    }

    private Set<Plant> resolverPlantas(List<Long> plantaIds) {
        if (plantaIds == null || plantaIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Plant> plantas = new HashSet<>();
        for (Long id : plantaIds) {
            plantas.add(plantService.buscarPorId(id));
        }
        return plantas;
    }

    /** Etiquetas de los días de la semana ISO (1=lunes..7=domingo). */
    private java.util.Map<Integer, String> diasSemana() {
        java.util.Map<Integer, String> dias = new java.util.LinkedHashMap<>();
        String[] nombres = new String[]{"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        for (int i = 0; i < nombres.length; i++) {
            dias.put(i + 1, nombres[i]);
        }
        return dias;
    }
}
