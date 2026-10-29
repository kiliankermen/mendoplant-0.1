package com.mendoplant.app.controller;

import com.mendoplant.app.dto.GrupoMulti;
import com.mendoplant.app.dto.PlantDTO;
import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.model.Category;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.Promocion;
import com.mendoplant.app.model.Order;
import com.mendoplant.app.model.OrderItem;
import com.mendoplant.app.repository.OrderItemRepository;
import com.mendoplant.app.repository.OrderRepository;
import com.mendoplant.app.security.UserPrincipal;
import com.mendoplant.app.service.CategoryService;
import com.mendoplant.app.service.GruposMultiplesBuilder;
import com.mendoplant.app.service.ImageStorageService;
import com.mendoplant.app.service.PlantService;
import com.mendoplant.app.service.PromocionService;
import com.mendoplant.app.service.SiteConfigService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dashboard del vendedor: estadísticas, publicaciones propias, carga de productos
 * y creación de promociones (combos, rebajas y envío gratis para el admin).
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final String CLAVE_ENVIO_GRATIS = "ENVIO_GRATIS_DESDE";
    private static final String COLOR_BANNER_DEFAULT = "#2f7d45";
    /** Clave de sesión donde se guardan los ids de publicaciones ocultas de la lista. */
    private static final String CLAVE_PUBLICACIONES_OCULTAS = "mpPublicacionesOcultas";
    /** Categorías que no se muestran como secciones en "Mis combos". */
    private static final Set<String> SECCIONES_OCULTAS_MIS_COMBOS =
            Set.of("Interior", "Exterior", "Suculentas", "Orquídeas", "Plantas", "Macetas", "Soportes", "Sustratos");
    private static final String[] MESES = {
            "ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic"
    };

    private final PlantService plantService;
    private final CategoryService categoryService;
    private final GruposMultiplesBuilder gruposMultiplesBuilder;
    private final ImageStorageService imageStorageService;
    private final PromocionService promocionService;
    private final SiteConfigService siteConfigService;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    // ------------------------------------------------------------------
    // Inicio del dashboard
    // ------------------------------------------------------------------
    @GetMapping
    public String index(@AuthenticationPrincipal UserPrincipal principal,
                        @RequestParam(name = "rango", defaultValue = "6m") String rango,
                        Model model) {
        Long filtroVentas = esAdmin(principal) ? -1L : principal.getId();

        List<Plant> base = esAdmin(principal)
                ? plantService.buscarConFiltros(null)
                : plantService.buscarPorVendedor(principal.getId());

        Map<Long, Object[]> filas = new LinkedHashMap<>();
        for (Object[] fila : orderItemRepository.ventasPorProducto(filtroVentas)) {
            filas.put(((Number) fila[0]).longValue(), fila);
        }

        List<Map<String, Object>> productos = new ArrayList<>();
        for (Plant p : base) {
            Object[] fila = filas.get(p.getId());
            BigDecimal monto;
            int cantidad;
            if (fila != null) {
                monto = aBigDecimal(fila[3]);
                cantidad = ((Number) fila[4]).intValue();
            } else {
                cantidad = p.getVentas() == null ? 0 : p.getVentas();
                monto = (p.getPrecio() == null ? BigDecimal.ZERO : p.getPrecio())
                        .multiply(BigDecimal.valueOf(cantidad));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("nombre", p.getNombreComun());
            item.put("imagenUrl", p.getImagenUrl());
            item.put("monto", monto);
            item.put("cantidad", cantidad);
            productos.add(item);
        }

        model.addAttribute("masGenero", productos.stream()
                .max(Comparator.comparing(p -> (BigDecimal) p.get("monto")))
                .orElse(null));
        model.addAttribute("masVendido", productos.stream()
                .max(Comparator.comparingInt(p -> (Integer) p.get("cantidad")))
                .orElse(null));
        model.addAttribute("menosVendido", productos.stream()
                .min(Comparator.comparingInt(p -> (Integer) p.get("cantidad")))
                .orElse(null));

        RangoVentas rv = resolverRango(rango, filtroVentas);
        model.addAttribute("chartLabels", rv.etiquetas());
        model.addAttribute("chartValues", rv.valores());
        model.addAttribute("chartCantidades", rv.cantidades());
        model.addAttribute("rangoSeleccionado", rv.clave());
        model.addAttribute("rangos", RANGOS_DISPONIBLES);
        model.addAttribute("totalProductos", base.size());
        model.addAttribute("seccion", "inicio");
        return "dashboard/index";
    }

    /** Ver registro histórico completo de ventas. */
    @GetMapping("/sales-history")
    public String salesHistory(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long filtroVentas = esAdmin(principal) ? -1L : principal.getId();

        List<Map<String, Object>> ventas = new ArrayList<>();
        BigDecimal totalFacturado = BigDecimal.ZERO;
        int totalUnidades = 0;
        for (Object[] fila : orderItemRepository.ordenesDelVendedor(filtroVentas)) {
            Long ordenId = ((Number) fila[0]).longValue();
            LocalDateTime fecha = aLocalDateTime(fila[1]);
            BigDecimal totalOrden = aBigDecimal(fila[2]);
            Long compradorId = fila[3] == null ? null : ((Number) fila[3]).longValue();
            int unidades = ((Number) fila[4]).intValue();
            BigDecimal montoVendedor = aBigDecimal(fila[5]);

            Order orden = orderRepository.findById(ordenId).orElse(null);
            String comprador = "—";
            if (orden != null && orden.getComprador() != null) {
                comprador = orden.getComprador().getNombre() != null
                        ? orden.getComprador().getNombre()
                        : orden.getComprador().getEmail();
            }

            List<Map<String, Object>> items = new ArrayList<>();
            if (orden != null) {
                for (OrderItem it : orden.getItems()) {
                    boolean propio = esAdmin(principal)
                            || (it.getPlant() != null && it.getPlant().getVendedor() != null
                                && principal.getId().equals(it.getPlant().getVendedor().getId()));
                    if (!propio) {
                        continue;
                    }
                    Map<String, Object> li = new LinkedHashMap<>();
                    li.put("producto", it.getPlant() != null ? it.getPlant().getNombreComun() : "—");
                    li.put("cantidad", it.getCantidad());
                    li.put("precio", it.getPrecioUnitario());
                    li.put("subtotal", it.getSubtotal());
                    items.add(li);
                }
            }

            Map<String, Object> fila2 = new LinkedHashMap<>();
            fila2.put("ordenId", ordenId);
            fila2.put("fecha", fecha);
            fila2.put("compradorId", compradorId);
            fila2.put("comprador", comprador);
            fila2.put("totalOrden", totalOrden);
            fila2.put("unidades", unidades);
            fila2.put("montoVendedor", montoVendedor);
            fila2.put("items", items);
            ventas.add(fila2);

            totalFacturado = totalFacturado.add(montoVendedor);
            totalUnidades += unidades;
        }

        model.addAttribute("ventas", ventas);
        model.addAttribute("totalVentas", ventas.size());
        model.addAttribute("totalFacturado", totalFacturado);
        model.addAttribute("totalUnidades", totalUnidades);
        model.addAttribute("seccion", "sales-history");
        return "dashboard/sales-history";
    }

    // ------------------------------------------------------------------
    // Rangos del gráfico de ventas
    // ------------------------------------------------------------------
    private static final List<String[]> RANGOS_DISPONIBLES = List.of(
            new String[]{"1m", "Último mes"},
            new String[]{"3m", "Últimos 3 meses"},
            new String[]{"6m", "Últimos 6 meses"},
            new String[]{"12m", "Últimos 12 meses"},
            new String[]{"2026", "Periodo 2026"}
    );

    private record RangoVentas(String clave, List<String> etiquetas,
                               List<BigDecimal> valores, List<Integer> cantidades) {
    }

    /**
     * Arma las etiquetas/valores/cantidades del gráfico según el rango elegido,
     * siempre a partir de datos reales de la base de datos.
     */
    private RangoVentas resolverRango(String rango, Long filtroVentas) {
        if (!esRangoValido(rango)) {
            rango = "6m";
        }
        return construirRango(rango, filtroVentas);
    }

    private boolean esRangoValido(String rango) {
        return RANGOS_DISPONIBLES.stream().anyMatch(r -> r[0].equals(rango));
    }

    private RangoVentas construirRango(String rango, Long filtroVentas) {
        List<String> etiquetas = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        List<Integer> cantidades = new ArrayList<>();

        if ("1m".equals(rango)) {
            LocalDate hoy = LocalDate.now();
            LocalDate desde = hoy.minusDays(29);
            Map<String, Object[]> porDia = new LinkedHashMap<>();
            for (Object[] fila : orderItemRepository.ventasPorDia(filtroVentas,
                    desde.atStartOfDay(), hoy.plusDays(1).atStartOfDay())) {
                porDia.put(String.valueOf(fila[0]), fila);
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            for (LocalDate d = desde; !d.isAfter(hoy); d = d.plusDays(1)) {
                etiquetas.add(d.format(fmt));
                Object[] fila = porDia.get(d.toString());
                valores.add(fila == null ? BigDecimal.ZERO : aBigDecimal(fila[1]));
                cantidades.add(fila == null ? 0 : ((Number) fila[2]).intValue());
            }
            return new RangoVentas(rango, etiquetas, valores, cantidades);
        }

        int meses;
        LocalDateTime desde;
        LocalDateTime hasta;
        if ("2026".equals(rango)) {
            meses = 12;
            desde = LocalDate.of(2026, 1, 1).atStartOfDay();
            hasta = LocalDate.of(2027, 1, 1).atStartOfDay();
        } else {
            meses = switch (rango) {
                case "3m" -> 3;
                case "12m" -> 12;
                default -> 6;
            };
            YearMonth hoy = YearMonth.now();
            desde = hoy.minusMonths(meses - 1).atDay(1).atStartOfDay();
            hasta = hoy.plusMonths(1).atDay(1).atStartOfDay();
        }

        Map<String, Object[]> porMes = new LinkedHashMap<>();
        for (Object[] fila : orderItemRepository.ventasPorMesRango(filtroVentas, desde, hasta)) {
            porMes.put(String.valueOf(fila[0]), fila);
        }

        for (int i = 0; i < meses; i++) {
            YearMonth mes = YearMonth.from(desde).plusMonths(i);
            etiquetas.add(MESES[mes.getMonthValue() - 1] + " " + String.valueOf(mes.getYear()).substring(2));
            Object[] fila = porMes.get(mes.toString());
            valores.add(fila == null ? BigDecimal.ZERO : aBigDecimal(fila[1]));
            cantidades.add(fila == null ? 0 : ((Number) fila[2]).intValue());
        }
        return new RangoVentas(rango, etiquetas, valores, cantidades);
    }

    // ------------------------------------------------------------------
    // Mis combos
    // ------------------------------------------------------------------
    @GetMapping("/mis-combos")
    public String misCombos(@AuthenticationPrincipal UserPrincipal principal,
                            Model model, HttpSession session) {
        Set<Long> ocultas = publicacionesOcultas(session);
        List<Plant> plantas = plantService.buscarPorVendedor(principal.getId()).stream()
                .filter(p -> p.getId() == null || !ocultas.contains(p.getId()))
                .toList();

        Map<String, List<Plant>> porTipo = new LinkedHashMap<>();
        for (Plant p : plantas) {
            String tipo = p.isEsCombo()
                    ? "Combos"
                    : (p.getCategoria() != null ? p.getCategoria().getNombre() : "Otros");
            if (SECCIONES_OCULTAS_MIS_COMBOS.contains(tipo)) {
                continue;
            }
            porTipo.computeIfAbsent(tipo, k -> new ArrayList<>()).add(p);
        }

        List<Map<String, Object>> secciones = new ArrayList<>();
        List<Plant> visibles = new ArrayList<>();
        porTipo.forEach((titulo, lista) -> {
            if (lista.isEmpty() || "Otros".equals(titulo)) {
                return;
            }
            Map<String, Object> seccion = new LinkedHashMap<>();
            seccion.put("titulo", titulo);
            seccion.put("plantas", lista);
            secciones.add(seccion);
            visibles.addAll(lista);
        });
        List<Plant> otros = porTipo.get("Otros");
        if (otros != null && !otros.isEmpty()) {
            Map<String, Object> seccion = new LinkedHashMap<>();
            seccion.put("titulo", "Otros");
            seccion.put("plantas", otros);
            secciones.add(seccion);
            visibles.addAll(otros);
        }

        model.addAttribute("secciones", secciones);
        model.addAttribute("totalPublicaciones", visibles.size());
        model.addAttribute("seccion", "mis-combos");
        return "dashboard/mis-combos";
    }

    /**
     * Elimina la publicación de la lista de "Mis combos" SIN borrarla de la base
     * de datos: el id queda marcado como oculto en la sesión del vendedor, por lo
     * que desaparece del listado (y de las recargas) hasta que se cierre la sesión.
     */
    @PostMapping("/mis-combos/eliminar/{id}")
    public String eliminarPublicacion(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal,
                                      HttpSession session,
                                      RedirectAttributes redirect) {
        boolean esPropia = plantService.buscarPorVendedor(principal.getId()).stream()
                .anyMatch(p -> p.getId() != null && p.getId().equals(id));
        if (!esPropia) {
            redirect.addFlashAttribute("error", "Esa publicación no pertenece a tu catálogo.");
            return "redirect:/dashboard/mis-combos";
        }

        Set<Long> ocultas = publicacionesOcultas(session);
        ocultas.add(id);
        session.setAttribute(CLAVE_PUBLICACIONES_OCULTAS, ocultas);
        redirect.addFlashAttribute("ok", "Publicación eliminada de la lista.");
        return "redirect:/dashboard/mis-combos";
    }

    /** Ids de publicaciones ocultas de la lista, guardados en la sesión (no en la base). */
    private Set<Long> publicacionesOcultas(HttpSession session) {
        Set<Long> ocultas = new LinkedHashSet<>();
        Object atributo = session.getAttribute(CLAVE_PUBLICACIONES_OCULTAS);
        if (atributo instanceof Set<?> guardado) {
            for (Object valor : guardado) {
                if (valor instanceof Number numero) {
                    ocultas.add(numero.longValue());
                }
            }
        }
        return ocultas;
    }

    @GetMapping("/mis-rebajas")
    public String misRebajas(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<Plant> propias = plantService.buscarPorVendedor(principal.getId());
        List<Promocion> rebajas = promocionService.listarRebajasVendedor(principal.getEmail());
        Set<Long> plantasEnPaquetes = promocionService.plantasEnPaquetesActivos(null);
        List<Plant> productos = propias.stream()
                .filter(p -> !p.isEsCombo())
                .filter(p -> p.getId() == null || !plantasEnPaquetes.contains(p.getId()))
                .toList();

        model.addAttribute("rebajas", rebajas);
        model.addAttribute("productos", productos);
        model.addAttribute("plantasEnPaquetes", plantasEnPaquetes);
        model.addAttribute("seccion", "mis-rebajas");
        return "dashboard/mis-rebajas";
    }

    // ------------------------------------------------------------------
    // Mis productos por categoría
    // ------------------------------------------------------------------
    @GetMapping("/mis-{tipo}")
    public String misProductosPorTipo(@PathVariable String tipo,
                                      @AuthenticationPrincipal UserPrincipal principal,
                                      Model model, HttpSession session) {
        if (!tipoValido(tipo)) {
            return "redirect:/dashboard";
        }
        String tipoNormalizado = normalizarTipo(tipo);
        String categoriaNombre = categoriaDe(tipoNormalizado).getNombre();
        Set<Long> ocultas = publicacionesOcultas(session);
        List<Plant> plantas = plantService.buscarPorVendedor(principal.getId()).stream()
                .filter(p -> p.getId() == null || !ocultas.contains(p.getId()))
                .filter(p -> !p.isEsCombo())
                .filter(p -> p.getCategoria() != null && categoriaNombre.equalsIgnoreCase(p.getCategoria().getNombre()))
                .toList();

        List<Map<String, Object>> secciones = new ArrayList<>();
        Map<String, Object> seccion = new LinkedHashMap<>();
        seccion.put("titulo", categoriaNombre);
        seccion.put("plantas", plantas);
        secciones.add(seccion);

        model.addAttribute("secciones", secciones);
        model.addAttribute("totalPublicaciones", plantas.size());
        model.addAttribute("seccion", "mis-" + tipo);
        model.addAttribute("categoriaNombre", categoriaNombre);
        return "dashboard/mis-productos";
    }

    // ------------------------------------------------------------------
    // Cargar productos
    // ------------------------------------------------------------------
    @GetMapping("/publicar-{tipo}")
    public String publicarForm(@PathVariable String tipo, Model model) {
        if (!tipoValido(tipo)) {
            return "redirect:/dashboard";
        }
        String tipoNormalizado = normalizarTipo(tipo);
        PlantDTO dto = new PlantDTO();
        dto.setStock(10);
        model.addAttribute("producto", dto);
        cargarFormularioPublicacion(model, tipoNormalizado, dto);
        return "dashboard/publicar";
    }

    @PostMapping("/publicar-{tipo}")
    public String publicar(@PathVariable String tipo,
                           @ModelAttribute("producto") PlantDTO dto,
                           @RequestParam(value = "archivos", required = false) List<MultipartFile> imagenes,
                           @AuthenticationPrincipal UserPrincipal principal,
                           Model model) {
        if (!tipoValido(tipo)) {
            return "redirect:/dashboard";
        }
        String tipoNormalizado = normalizarTipo(tipo);

        List<MultipartFile> validas = imagenes == null ? List.of()
                : imagenes.stream().filter(f -> f != null && !f.isEmpty()).toList();
        String error = validarProducto(dto, validas);
        if (error != null) {
            model.addAttribute("error", error);
            cargarFormularioPublicacion(model, tipoNormalizado, dto);
            return "dashboard/publicar";
        }

        List<String> urls = new ArrayList<>();
        try {
            for (MultipartFile archivo : validas) {
                urls.add(imageStorageService.guardar(archivo));
            }
        } catch (IllegalArgumentException e) {
            urls.forEach(imageStorageService::eliminar);
            model.addAttribute("error", e.getMessage());
            cargarFormularioPublicacion(model, tipoNormalizado, dto);
            return "dashboard/publicar";
        }

        dto.setImagenes(urls);
        dto.setImagenUrl(urls.get(0));
        dto.setCategoriaId(categoriaDe(tipoNormalizado).getId());
        dto.setEsCombo(false);
        plantService.publicar(dto, principal.getId());
        return "redirect:/dashboard/mis-" + tipo;
    }

    private void cargarFormularioPublicacion(Model model, String tipo, PlantDTO dto) {
        String tipoNormalizado = normalizarTipo(tipo);
        model.addAttribute("tipo", tipoNormalizado);
        model.addAttribute("tipoTitulo", tipoTitulo(tipoNormalizado));
        model.addAttribute("categoriaNombre", categoriaDe(tipoNormalizado).getNombre());
        model.addAttribute("grupos", gruposDe(tipoNormalizado, dto));
        model.addAttribute("seccion", "publicar-" + tipoNormalizado);
    }

    private List<GrupoMulti> gruposDe(String tipo, PlantDTO dto) {
        String t = normalizarTipo(tipo);
        Set<String> campos = switch (t) {
            case "maceta" -> Set.of("tiposMaceta", "tamanos");
            case "soporte" -> Set.of("tiposAccesorio", "tamanos");
            case "sustrato" -> Set.of("tiposSustrato", "tamanos");
            default -> Set.of("ambientes", "climas", "nivelesCuidado", "tiposPlanta", "luces", "tamanos");
        };
        return gruposMultiplesBuilder.gruposFormulario(dto).stream()
                .filter(g -> campos.contains(g.campo()))
                .toList();
    }

    private String validarProducto(PlantDTO dto, List<MultipartFile> imagenes) {
        if (dto.getNombreComun() == null || dto.getNombreComun().isBlank()) {
            return "El nombre común es obligatorio.";
        }
        if (dto.getPrecio() == null || dto.getPrecio().signum() <= 0) {
            return "El precio debe ser mayor a 0.";
        }
        if (dto.getStock() == null || dto.getStock() < 10) {
            return "El stock mínimo para publicar es de 10 unidades.";
        }
        if (imagenes.size() < 2) {
            return "Tenés que cargar al menos 2 imágenes para publicar el producto.";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Combos
    // ------------------------------------------------------------------
    @GetMapping("/promociones/crear-combos")
    public String combos(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<Plant> productos = plantService.buscarPorVendedor(principal.getId());
        model.addAttribute("productos", productos.stream().filter(p -> !p.isEsCombo()).toList());
        model.addAttribute("combos", productos.stream().filter(Plant::isEsCombo).toList());
        model.addAttribute("seccion", "combos");
        return "dashboard/combos";
    }

    @PostMapping("/promociones/crear-combos")
    public String crearCombo(@RequestParam(value = "productos", required = false) List<Long> ids,
                             @RequestParam(value = "nombre", required = false) String nombreCombo,
                             @AuthenticationPrincipal UserPrincipal principal,
                             RedirectAttributes redirect) {
        List<Plant> seleccion = seleccionDelVendedor(principal, ids);
        if (seleccion.size() < 2) {
            redirect.addFlashAttribute("error", "Seleccioná al menos 2 productos para crear el combo.");
            return "redirect:/dashboard/promociones/crear-combos";
        }
        if (nombreCombo == null || nombreCombo.isBlank()) {
            redirect.addFlashAttribute("error", "Ingresá un nombre para el combo.");
            return "redirect:/dashboard/promociones/crear-combos";
        }

        String nombre = nombreCombo.trim();
        if (nombre.length() > 115) {
            nombre = nombre.substring(0, 112) + "...";
        }

        PlantDTO dto = new PlantDTO();
        dto.setNombreComun(nombre);
        dto.setNombreCientifico(nombre);
        dto.setDescripcion("Incluye: " + seleccion.stream().map(Plant::getNombreComun)
                .reduce((a, b) -> a + ", " + b).orElse(""));
        dto.setPrecio(seleccion.stream()
                .map(p -> p.getPrecio() == null ? BigDecimal.ZERO : p.getPrecio())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setStock(seleccion.stream()
                .map(p -> p.getStock() == null ? 0 : p.getStock())
                .min(Integer::compareTo).orElse(0));
        dto.setCategoriaId(seleccion.get(0).getCategoria() != null
                ? seleccion.get(0).getCategoria().getId() : null);
        dto.setEsCombo(true);
        dto.setImagenes(imagenesDe(seleccion));

        for (Plant p : seleccion) {
            dto.getTamanos().addAll(p.getTamanos());
            dto.getLuces().addAll(p.getLuces());
            dto.getAmbientes().addAll(p.getAmbientes());
            dto.getClimas().addAll(p.getClimas());
            dto.getNivelesCuidado().addAll(p.getNivelesCuidado());
            dto.getTiposPlanta().addAll(p.getTiposPlanta());
            dto.getTiposMaceta().addAll(p.getTiposMaceta());
            dto.getTiposSustrato().addAll(p.getTiposSustrato());
            dto.getTiposAccesorio().addAll(p.getTiposAccesorio());
        }

        plantService.publicar(dto, principal.getId());
        redirect.addFlashAttribute("ok", "Combo creado correctamente.");
        return "redirect:/dashboard/promociones/crear-combos";
    }

    // ------------------------------------------------------------------
    // Rebajas
    // ------------------------------------------------------------------
    @GetMapping("/promociones/crear-rebajas")
    public String rebajas(@RequestParam(value = "promo", required = false) Long promoId,
                          @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Promocion rebaja = null;
        if (promoId != null) {
            try {
                rebaja = promocionService.buscarRebajaVendedor(promoId, principal.getEmail());
            } catch (RuntimeException ignored) {
                rebaja = null;
            }
        }
        Set<Long> plantasEnPaquetes = promocionService.plantasEnPaquetesActivos(
                rebaja != null ? rebaja.getId() : null);
        List<Plant> productos = plantService.buscarPorVendedor(principal.getId()).stream()
                .filter(p -> !p.isEsCombo())
                .filter(p -> p.getId() == null || !plantasEnPaquetes.contains(p.getId()))
                .toList();

        model.addAttribute("productos", productos);
        model.addAttribute("rebaja", rebaja);
        model.addAttribute("nombrePromo", rebaja != null ? rebaja.getNombre() : "");
        model.addAttribute("descripcionPromo", rebaja != null ? rebaja.getDescripcion() : "");
        model.addAttribute("colorBanner", rebaja != null && rebaja.getColorBanner() != null
                ? rebaja.getColorBanner() : COLOR_BANNER_DEFAULT);
        model.addAttribute("porcentajeActual", rebaja != null ? rebaja.getPorcentaje() : null);
        model.addAttribute("plantasEnPaquetes", plantasEnPaquetes);
        model.addAttribute("seleccionadas", rebaja == null ? Set.of()
                : rebaja.getPlantas().stream().map(Plant::getId).collect(java.util.stream.Collectors.toSet()));
        model.addAttribute("seccion", "rebajas");
        return "dashboard/rebajas";
    }

    @PostMapping("/promociones/crear-rebajas")
    public String aplicarRebaja(@RequestParam(value = "promoId", required = false) Long promoId,
                                @RequestParam(value = "nombre", required = false) String nombre,
                                @RequestParam(value = "descripcion", required = false) String descripcion,
                                @RequestParam(value = "colorBanner", required = false) String colorBanner,
                                @RequestParam(value = "productos", required = false) List<Long> ids,
                                @RequestParam(value = "porcentaje", required = false) BigDecimal porcentaje,
                                @AuthenticationPrincipal UserPrincipal principal,
                                RedirectAttributes redirect) {
        if (porcentaje == null || porcentaje.signum() <= 0 || porcentaje.compareTo(new BigDecimal("90")) > 0) {
            redirect.addFlashAttribute("error", "El descuento debe estar entre 1% y 90%.");
            return "redirect:/dashboard/promociones/crear-rebajas";
        }
        List<Plant> seleccion = seleccionDelVendedor(principal, ids).stream()
                .filter(p -> !p.isEsCombo())
                .toList();
        if (seleccion.isEmpty()) {
            redirect.addFlashAttribute("error", "Seleccioná al menos un producto para aplicar la rebaja.");
            return "redirect:/dashboard/promociones/crear-rebajas";
        }
        String nombreFinal = nombre == null || nombre.isBlank()
                ? nombreRebaja(principal)
                : nombre.trim();
        if (nombreFinal.length() > 120) {
            redirect.addFlashAttribute("error", "El nombre de la promoción no puede superar los 120 caracteres.");
            return "redirect:/dashboard/promociones/crear-rebajas";
        }
        String descripcionFinal = descripcion == null ? null : descripcion.trim();
        if (descripcionFinal != null && descripcionFinal.length() > 100) {
            redirect.addFlashAttribute("error", "La descripción no puede superar los 100 caracteres.");
            return "redirect:/dashboard/promociones/crear-rebajas";
        }
        try {
            promocionService.guardarRebaja(promoId, nombreFinal, descripcionFinal, porcentaje,
                    colorBannerValido(colorBanner), new HashSet<>(seleccion), principal.getEmail());
            redirect.addFlashAttribute("ok", "Rebaja guardada para " + seleccion.size() + " producto(s).");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", "No se pudo guardar la rebaja.");
        }
        return "redirect:/dashboard/mis-rebajas";
    }

    @PostMapping("/promociones/rebajas/{id}/porcentaje")
    public String actualizarPorcentajeRebaja(@PathVariable Long id,
                                             @RequestParam(value = "porcentaje", required = false) BigDecimal porcentaje,
                                             @AuthenticationPrincipal UserPrincipal principal,
                                             RedirectAttributes redirect) {
        if (porcentaje == null || porcentaje.signum() <= 0 || porcentaje.compareTo(new BigDecimal("90")) > 0) {
            redirect.addFlashAttribute("error", "El descuento debe estar entre 1% y 90%.");
            return "redirect:/dashboard/mis-rebajas";
        }
        try {
            promocionService.actualizarPorcentajeRebaja(id, porcentaje, principal.getEmail());
            redirect.addFlashAttribute("ok", "Porcentaje actualizado.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", "No se pudo actualizar el porcentaje.");
        }
        return "redirect:/dashboard/mis-rebajas";
    }

    @PostMapping("/promociones/rebajas/{id}/datos")
    public String actualizarDatosRebaja(@PathVariable Long id,
                                        @RequestParam(value = "nombre", required = false) String nombre,
                                        @RequestParam(value = "descripcion", required = false) String descripcion,
                                        @RequestParam(value = "colorBanner", required = false) String colorBanner,
                                        @AuthenticationPrincipal UserPrincipal principal,
                                        RedirectAttributes redirect) {
        try {
            promocionService.actualizarDatosRebaja(id, nombre, descripcion,
                    colorBannerValido(colorBanner), principal.getEmail());
            redirect.addFlashAttribute("ok", "Promoción actualizada.");
        } catch (IllegalArgumentException | BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", "No se pudo actualizar la promoción.");
        }
        return "redirect:/dashboard/mis-rebajas";
    }

    @PostMapping("/promociones/rebajas/{id}/plantas/{plantaId}/agregar")
    public String agregarPlantaDeRebaja(@PathVariable Long id,
                                        @PathVariable Long plantaId,
                                        @AuthenticationPrincipal UserPrincipal principal,
                                        RedirectAttributes redirect) {
        Plant planta = plantService.buscarPorVendedor(principal.getId()).stream()
                .filter(p -> !p.isEsCombo())
                .filter(p -> p.getId() != null && p.getId().equals(plantaId))
                .findFirst()
                .orElse(null);
        if (planta == null) {
            redirect.addFlashAttribute("error", "La planta no pertenece a tu catálogo.");
            return "redirect:/dashboard/mis-rebajas";
        }
        try {
            promocionService.agregarPlantaRebaja(id, planta, principal.getEmail());
            redirect.addFlashAttribute("ok", "Planta agregada al paquete de promoción.");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", "No se pudo agregar la planta al paquete.");
        }
        return "redirect:/dashboard/mis-rebajas";
    }

    @PostMapping("/promociones/rebajas/{id}/eliminar")
    public String eliminarRebaja(@PathVariable Long id,
                                 @AuthenticationPrincipal UserPrincipal principal,
                                 RedirectAttributes redirect) {
        try {
            promocionService.eliminarRebaja(id, principal.getEmail());
            redirect.addFlashAttribute("ok", "Paquete de promoción eliminado.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", "No se pudo eliminar el paquete de promoción.");
        }
        return "redirect:/dashboard/mis-rebajas";
    }

    @PostMapping("/promociones/rebajas/{id}/plantas/{plantaId}/eliminar")
    public String quitarPlantaDeRebaja(@PathVariable Long id,
                                       @PathVariable Long plantaId,
                                       @AuthenticationPrincipal UserPrincipal principal,
                                       RedirectAttributes redirect) {
        try {
            Promocion promo = promocionService.quitarPlantaRebaja(id, plantaId, principal.getEmail());
            if (promo.getPlantas().isEmpty()) {
                redirect.addFlashAttribute("ok", "Se quitó la última planta y el paquete quedó eliminado.");
            } else {
                redirect.addFlashAttribute("ok", "Planta quitada de la promoción.");
            }
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", "No se pudo quitar la planta de la promoción.");
        }
        return "redirect:/dashboard/mis-rebajas";
    }

    // ------------------------------------------------------------------
    // Envíos gratis (solo ADMIN)
    // ------------------------------------------------------------------
    @GetMapping("/promociones/envios-gratis")
    public String enviosGratis(Model model) {
        model.addAttribute("envioGratisDesde",
                siteConfigService.obtener(CLAVE_ENVIO_GRATIS, "90000"));
        model.addAttribute("seccion", "envios-gratis");
        return "dashboard/envios-gratis";
    }

    @PostMapping("/promociones/envios-gratis")
    public String guardarEnviosGratis(@RequestParam BigDecimal monto, RedirectAttributes redirect) {
        if (monto == null || monto.signum() < 0) {
            redirect.addFlashAttribute("error", "El monto debe ser un número positivo.");
            return "redirect:/dashboard/promociones/envios-gratis";
        }
        siteConfigService.guardar(CLAVE_ENVIO_GRATIS, monto.stripTrailingZeros().toPlainString());
        redirect.addFlashAttribute("ok", "Umbral de envío gratis actualizado.");
        return "redirect:/dashboard/promociones/envios-gratis";
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private boolean esAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private boolean tipoValido(String tipo) {
        if (tipo == null) {
            return false;
        }
        String t = tipo.toLowerCase();
        return "planta".equals(t) || "plantas".equals(t)
                || "maceta".equals(t) || "macetas".equals(t)
                || "soporte".equals(t) || "soportes".equals(t)
                || "sustrato".equals(t) || "sustratos".equals(t);
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null) {
            return null;
        }
        String t = tipo.toLowerCase();
        return switch (t) {
            case "plantas" -> "planta";
            case "macetas" -> "maceta";
            case "soportes" -> "soporte";
            case "sustratos" -> "sustrato";
            default -> t;
        };
    }

    private String tiendaPath(String tipo) {
        if (tipo == null) {
            return "plantas";
        }
        String t = tipo.toLowerCase();
        return switch (normalizarTipo(t)) {
            case "maceta" -> "macetas";
            case "soporte" -> "soportes";
            case "sustrato" -> "sustratos";
            default -> "plantas";
        };
    }

    private String tipoTitulo(String tipo) {
        if (tipo == null) {
            return "planta";
        }
        String t = tipo.toLowerCase();
        return switch (normalizarTipo(t)) {
            case "maceta" -> "maceta";
            case "soporte" -> "soporte";
            case "sustrato" -> "sustrato";
            default -> "planta";
        };
    }

    private Category categoriaDe(String tipo) {
        if (tipo == null) {
            tipo = "planta";
        }
        String t = tipo.toLowerCase();
        String nombre = switch (normalizarTipo(t)) {
            case "maceta" -> "Macetas";
            case "soporte" -> "Soportes";
            case "sustrato" -> "Sustratos";
            default -> "Plantas";
        };
        return categoryService.listarActivas().stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No existe la categoría " + nombre));
    }

    private List<Plant> seleccionDelVendedor(UserPrincipal principal, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> buscados = new LinkedHashSet<>(ids);
        return plantService.buscarPorVendedor(principal.getId()).stream()
                .filter(p -> buscados.contains(p.getId()))
                .toList();
    }

    private List<String> imagenesDe(List<Plant> seleccion) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        for (Plant p : seleccion) {
            if (p.getImagenes() != null) {
                urls.addAll(p.getImagenes());
            }
            if (p.getImagenUrl() != null && !p.getImagenUrl().isBlank()) {
                urls.add(p.getImagenUrl());
            }
        }
        if (urls.isEmpty()) {
            urls.add("/images/placeholder-planta.svg");
        }
        return new ArrayList<>(urls);
    }

    private String nombreRebaja(UserPrincipal principal) {
        return "Rebajas de " + principal.getNombre();
    }

    private String colorBannerValido(String color) {
        return color != null && color.matches("#[0-9a-fA-F]{6}")
                ? color.toLowerCase()
                : COLOR_BANNER_DEFAULT;
    }

    private LocalDateTime aLocalDateTime(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (valor instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        try {
            return LocalDateTime.parse(valor.toString().replace(' ', 'T'));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private BigDecimal aBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal bd) {
            return bd;
        }
        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
