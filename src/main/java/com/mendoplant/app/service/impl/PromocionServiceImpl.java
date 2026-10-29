package com.mendoplant.app.service.impl;

import com.mendoplant.app.dto.PromocionDTO;
import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.OfertaDia;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.Promocion;
import com.mendoplant.app.model.TipoAplicacion;
import com.mendoplant.app.model.TipoClima;
import com.mendoplant.app.repository.OfertaDiaRepository;
import com.mendoplant.app.repository.PlantRepository;
import com.mendoplant.app.repository.PromocionRepository;
import com.mendoplant.app.service.PromocionService;
import com.mendoplant.app.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service("promocionService")
@RequiredArgsConstructor
public class PromocionServiceImpl implements PromocionService {

    /** Primavera en Argentina (hemisferio sur): septiembre, octubre y noviembre. */
    private static final int[] MESES_PRIMAVERA = {9, 10, 11};

    private static final BigDecimal CIEN = new BigDecimal("100");

    /** Clave en site_config con el umbral de envío gratis. */
    private static final String CLAVE_GRATIS_DESDE = "ENVIO_GRATIS_DESDE";

    private static final Map<String, String> SLUGS_ESTATICOS = Map.of(
            "combos", "Combos que arman jardín",
            "oferta-dia", "Oferta del día"
    );

    private final PromocionRepository promocionRepository;
    private final OfertaDiaRepository ofertaDiaRepository;
    private final PlantRepository plantRepository;
    private final SiteConfigService siteConfigService;

    /** Umbral de envío gratis de respaldo (application.yml/entorno) si no hay fila en site_config. */
    @Value("${app.envio.gratis-desde:0}")
    private BigDecimal gratisDesdeRespaldo;

    @Override
    public boolean isPrimavera() {
        int mes = LocalDate.now().getMonthValue();
        for (int m : MESES_PRIMAVERA) {
            if (mes == m) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BigDecimal porcentajeDescuento(Plant plant) {
        if (plant == null || !plant.isActivo()) {
            return BigDecimal.ZERO;
        }
        BigDecimal mejor = BigDecimal.ZERO;
        int dia = diaSemanaISO();

        java.util.Optional<OfertaDia> ofertaOpt = ofertaDiaRepository.findByDiaSemanaAndActivaTrue(dia);
        if (ofertaOpt.isPresent()) {
            OfertaDia oferta = ofertaOpt.get();
            if (oferta.getPlanta() != null
                    && java.util.Objects.equals(oferta.getPlanta().getId(), plant.getId())
                    && oferta.getPorcentaje() != null
                    && oferta.getPorcentaje().compareTo(mejor) > 0) {
                mejor = oferta.getPorcentaje();
            }
        }

        for (Promocion promo : promocionRepository.findByActivaTrue()) {
            if (promo.getPorcentaje() == null || promo.getPorcentaje().compareTo(mejor) <= 0) {
                continue;
            }
            if (vigenteHoy(promo) && aplicaA(promo, plant)) {
                mejor = promo.getPorcentaje();
            }
        }
        return clampPorcentaje(mejor);
    }

    @Override
    public int porcentajeDescuentoVisible(Plant plant) {
        return promocionPorcentaje(porcentajeDescuento(plant));
    }

    @Override
    public boolean tieneDescuento(Plant plant) {
        return porcentajeDescuento(plant).signum() > 0;
    }

    @Override
    public BigDecimal precioConDescuento(Plant plant) {
        if (plant == null || plant.getPrecio() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal pct = porcentajeDescuento(plant);
        if (pct.signum() <= 0) {
            return plant.getPrecio();
        }
        BigDecimal factor = CIEN.subtract(pct).divide(CIEN, 4, RoundingMode.HALF_UP);
        return plant.getPrecio().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean esOfertaDelDia(Plant plant) {
        if (plant == null) {
            return false;
        }
        return ofertaDiaRepository.findByDiaSemanaAndActivaTrue(diaSemanaISO())
                .map(oferta -> oferta.getPlanta() != null
                        && java.util.Objects.equals(oferta.getPlanta().getId(), plant.getId()))
                .orElse(false);
    }

    @Override
    public List<Plant> ofertasDelDia() {
        return ofertaDiaRepository.findByDiaSemanaAndActivaTrue(diaSemanaISO())
                .map(oferta -> plantRepository.findById(oferta.getPlanta().getId())
                        .filter(Plant::isActivo)
                        .map(List::of)
                        .orElseGet(List::of))
                .orElseGet(List::of);
    }

    @Override
    public List<PromocionDTO> promocionesActivas() {
        List<PromocionDTO> promos = new ArrayList<>();

        List<Promocion> vigentes = promocionRepository.findByActivaTrue().stream()
                .filter(this::vigenteHoy)
                .filter(this::esPromocionPublicable)
                .sorted(Comparator.comparing(Promocion::getPorcentaje).reversed())
                .toList();

        for (int i = 0; i < vigentes.size(); i++) {
            Promocion promo = vigentes.get(i);
            boolean esPrimavera = promo.getNombre().toLowerCase().contains("primavera");
            promos.add(PromocionDTO.builder()
                    .titulo(promo.getNombre())
                    .subtitulo(descripcionPromo(promo))
                    .badge("-" + formatoPorcentaje(promo.getPorcentaje()) + "%")
                    .color(esPrimavera ? "primavera" : (i % 2 == 0 ? "oferta" : "combo"))
                    .slug("promocion-" + promo.getId())
                    .colorHex(promo.getColorBanner())
                    .imagenUrl(esPrimavera ? "/images/promo-primavera.svg" : "/images/promo-oferta.svg")
                    .activa(true)
                    .build());
        }

        promos.add(PromocionDTO.builder()
                .titulo("Combos que arman jardín")
                .subtitulo("Plantas + macetas + accesorios a precio especial")
                .badge("Combos")
                .color("combo")
                .slug("combos")
                .imagenUrl("/images/promo-combos.svg")
                .activa(true)
                .build());

        promos.add(PromocionDTO.builder()
                .titulo("Oferta del día")
                .subtitulo("Una planta destacada con descuento, renovada todos los días")
                .badge("Hoy")
                .color("oferta")
                .slug("oferta-dia")
                .imagenUrl("/images/promo-oferta.svg")
                .activa(!ofertasDelDia().isEmpty())
                .build());

        promos.add(PromocionDTO.builder()
                .titulo("Envíos gratis")
                .subtitulo("Compras mayores a $" + formatoMontoPesos(envioGratisDesde()) + " sin coste de envío")
                .badge("Envíos")
                .color("envio")
                .slug("envio-gratis")
                .imagenUrl("/images/promo-oferta.svg")
                .botonTexto("Ir a carrito")
                .botonEnlace("/carrito")
                .activa(true)
                .build());

        return promos;
    }

    @Override
    public List<Plant> filtrarPorPromo(String slug, List<Plant> plantas) {
        if (slug == null || plantas == null || plantas.isEmpty()) {
            return List.of();
        }
        if ("combos".equals(slug)) {
            return plantas.stream().filter(Plant::isEsCombo).toList();
        }
        if ("oferta-dia".equals(slug)) {
            return plantas.stream().filter(this::esOfertaDelDia).toList();
        }
        if (slug.startsWith("promocion-")) {
            Long id = extraerId(slug);
            if (id == null) {
                return List.of();
            }
            Promocion promo = promocionRepository.findById(id).orElse(null);
            if (promo == null || !vigenteHoy(promo)) {
                return List.of();
            }
            return plantas.stream()
                    .filter(p -> aplicaA(promo, p))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public String tituloPromo(String slug) {
        if (slug == null) {
            return "Promoción";
        }
        String estatico = SLUGS_ESTATICOS.get(slug);
        if (estatico != null) {
            return estatico;
        }
        if (slug.startsWith("promocion-")) {
            Long id = extraerId(slug);
            if (id != null) {
                return promocionRepository.findById(id)
                        .map(Promocion::getNombre)
                        .orElse("Promoción");
            }
        }
        return "Promoción";
    }

    // ------------------------------------------------------------------
    // Gestión ADMIN
    // ------------------------------------------------------------------

    @Override
    public List<Promocion> listarPromociones() {
        return promocionRepository.findAll().stream()
                .sorted(Comparator.comparing(Promocion::getId))
                .toList();
    }

    @Override
    public Promocion buscarPorId(Long id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con id: " + id));
    }

    @Override
    @Transactional
    public Promocion guardarPromocion(Promocion promocion) {
        if (promocion.isActiva() && promocion.getTipoAplicacion() == TipoAplicacion.PLANTAS) {
            validarPlantasEnUnSoloPaquete(promocion.getPlantas(), promocion.getId());
        }
        return promocionRepository.save(promocion);
    }

    @Override
    @Transactional
    public void desactivarPromocion(Long id) {
        Promocion promo = promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con id: " + id));
        promo.setActiva(false);
        promocionRepository.save(promo);
    }

    @Override
    public List<OfertaDia> listarOfertasDia() {
        return ofertaDiaRepository.findAllByOrderByDiaSemanaAsc();
    }

    @Override
    @Transactional
    public OfertaDia guardarOfertaDia(int diaSemana, Long plantaId, BigDecimal porcentaje, boolean activa) {
        if (diaSemana < 1 || diaSemana > 7) {
            throw new IllegalArgumentException("Día de semana inválido (1=lunes..7=domingo): " + diaSemana);
        }
        Plant planta = plantRepository.findById(plantaId)
                .orElseThrow(() -> new ResourceNotFoundException("Planta no encontrada con id: " + plantaId));
        BigDecimal pct = clampPorcentaje(porcentaje == null ? BigDecimal.ZERO : porcentaje);

        OfertaDia oferta = ofertaDiaRepository.findByDiaSemana(diaSemana).orElseGet(() ->
                OfertaDia.builder().diaSemana(diaSemana).build());
        oferta.setPlanta(planta);
        oferta.setPorcentaje(pct);
        oferta.setActiva(activa);
        return ofertaDiaRepository.save(oferta);
    }

    @Override
    public List<Promocion> listarRebajasVendedor(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return promocionRepository.findByActivaTrueAndCreatedBy(email).stream()
                .filter(p -> p.getTipoAplicacion() == TipoAplicacion.PLANTAS)
                .sorted(Comparator.comparing(Promocion::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Promocion::getId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Promocion buscarRebajaVendedor(Long id, String email) {
        if (id == null || email == null || email.isBlank()) {
            throw new ResourceNotFoundException("Rebaja no encontrada");
        }
        Promocion promo = promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rebaja no encontrada con id: " + id));
        if (!email.equals(promo.getCreatedBy()) || promo.getTipoAplicacion() != TipoAplicacion.PLANTAS) {
            throw new ResourceNotFoundException("Rebaja no encontrada con id: " + id);
        }
        return promo;
    }

    @Override
    @Transactional
    public Promocion guardarRebaja(Long id, String nombre, String descripcion, BigDecimal porcentaje,
                                   String colorBanner, Set<Plant> plantas, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al vendedor.");
        }
        Promocion promo;
        if (id != null) {
            promo = buscarRebajaVendedor(id, email);
        } else {
            promo = promocionRepository.findFirstByNombreAndActivaTrueAndCreatedBy(nombre, email).orElseGet(() ->
                    Promocion.builder()
                            .nombre(nombre)
                            .diasSemana("")
                            .tipoAplicacion(TipoAplicacion.PLANTAS)
                            .createdBy(email)
                            .activa(true)
                            .build());
        }
        Set<Plant> nuevasPlantas = plantas == null ? new java.util.HashSet<>() : plantas;
        validarPlantasEnUnSoloPaquete(nuevasPlantas, promo.getId());
        promo.setNombre(nombre);
        promo.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        promo.setPorcentaje(clampPorcentaje(porcentaje));
        promo.setColorBanner(colorBanner == null || colorBanner.isBlank() ? null : colorBanner);
        promo.setPlantas(nuevasPlantas);
        promo.setTipoAplicacion(TipoAplicacion.PLANTAS);
        promo.setActiva(true);
        return promocionRepository.save(promo);
    }

    @Override
    @Transactional
    public Promocion actualizarPorcentajeRebaja(Long id, BigDecimal porcentaje, String email) {
        Promocion promo = buscarRebajaVendedor(id, email);
        promo.setPorcentaje(clampPorcentaje(porcentaje));
        return promocionRepository.save(promo);
    }

    @Override
    @Transactional
    public Promocion actualizarDatosRebaja(Long id, String nombre, String descripcion,
                                           String colorBanner, String email) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la promoción es obligatorio.");
        }
        String nombreLimpio = nombre.trim();
        if (nombreLimpio.length() > 120) {
            throw new IllegalArgumentException("El nombre no puede superar los 120 caracteres.");
        }
        String descripcionLimpia = descripcion == null ? null : descripcion.trim();
        if (descripcionLimpia != null && descripcionLimpia.length() > 100) {
            throw new IllegalArgumentException("La descripción no puede superar los 100 caracteres.");
        }
        Promocion promo = buscarRebajaVendedor(id, email);
        promo.setNombre(nombreLimpio);
        promo.setDescripcion(descripcionLimpia == null || descripcionLimpia.isBlank()
                ? null : descripcionLimpia);
        promo.setColorBanner(colorBanner == null || colorBanner.isBlank() ? null : colorBanner);
        return promocionRepository.save(promo);
    }

    @Override
    @Transactional
    public Promocion agregarPlantaRebaja(Long id, Plant planta, String email) {
        if (planta == null) {
            throw new IllegalArgumentException("La planta es obligatoria.");
        }
        Promocion promo = buscarRebajaVendedor(id, email);
        Set<Plant> nuevas = promo.getPlantas() == null
                ? new java.util.HashSet<>()
                : new java.util.HashSet<>(promo.getPlantas());
        boolean yaEsta = nuevas.stream()
                .anyMatch(p -> java.util.Objects.equals(p.getId(), planta.getId()));
        if (yaEsta) {
            return promo;
        }
        nuevas.add(planta);
        validarPlantasEnUnSoloPaquete(nuevas, promo.getId());
        promo.setPlantas(nuevas);
        promo.setActiva(true);
        return promocionRepository.save(promo);
    }

    @Override
    public Set<Long> plantasEnPaquetesActivos(Long excluirPromoId) {
        Set<Long> ids = new java.util.HashSet<>();
        for (Promocion promo : promocionRepository.findByActivaTrue()) {
            if (promo.getTipoAplicacion() != TipoAplicacion.PLANTAS
                    || (excluirPromoId != null && excluirPromoId.equals(promo.getId()))
                    || promo.getPlantas() == null) {
                continue;
            }
            promo.getPlantas().stream()
                    .map(Plant::getId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(ids::add);
        }
        return ids;
    }

    @Override
    @Transactional
    public Promocion quitarPlantaRebaja(Long id, Long plantaId, String email) {
        Promocion promo = buscarRebajaVendedor(id, email);
        if (plantaId == null) {
            throw new IllegalArgumentException("La planta es obligatoria.");
        }
        promo.getPlantas().removeIf(p -> java.util.Objects.equals(p.getId(), plantaId));
        if (promo.getPlantas().isEmpty()) {
            promo.setActiva(false);
        }
        return promocionRepository.save(promo);
    }

    @Override
    @Transactional
    public void eliminarRebaja(Long id, String email) {
        Promocion promo = buscarRebajaVendedor(id, email);
        promo.setActiva(false);
        promocionRepository.save(promo);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Día ISO: 1=lunes .. 7=domingo. */
    private int diaSemanaISO() {
        return LocalDate.now().getDayOfWeek().getValue();
    }

    /** ¿La promoción está vigente hoy? (activa + ventana de fechas + día de la semana). */
    private boolean vigenteHoy(Promocion promo) {
        LocalDate hoy = LocalDate.now();
        if (!promo.isActiva()) {
            return false;
        }
        if (promo.getFechaInicio() != null && hoy.isBefore(promo.getFechaInicio())) {
            return false;
        }
        if (promo.getFechaFin() != null && hoy.isAfter(promo.getFechaFin())) {
            return false;
        }
        String dias = promo.getDiasSemana();
        if (dias == null || dias.isBlank()) {
            return true;
        }
        int dia = diaSemanaISO();
        return Arrays.stream(dias.split(","))
                .map(String::trim)
                .anyMatch(d -> d.matches("\\d+") && Integer.parseInt(d) == dia);
    }

    /** ¿Aplica la promoción a esta planta según su alcance? */
    private boolean aplicaA(Promocion promo, Plant plant) {
        if (promo.getClima() != null && !plant.getClimas().contains(promo.getClima())) {
            return false;
        }
        if (promo.getTipoAplicacion() == null) {
            return false;
        }
        return switch (promo.getTipoAplicacion()) {
            case TODAS -> true;
            case CATEGORIA -> promo.getCategoria() != null
                    && plant.getCategoria() != null
                    && java.util.Objects.equals(promo.getCategoria().getId(), plant.getCategoria().getId());
            case PLANTAS -> !promo.getPlantas().isEmpty()
                    && promo.getPlantas().stream()
                    .anyMatch(pp -> java.util.Objects.equals(pp.getId(), plant.getId()));
        };
    }

    private BigDecimal clampPorcentaje(BigDecimal pct) {
        if (pct == null) {
            return BigDecimal.ZERO;
        }
        if (pct.signum() < 0) {
            return BigDecimal.ZERO;
        }
        if (pct.compareTo(CIEN) > 0) {
            return CIEN;
        }
        return pct;
    }

    private String descripcionPromo(Promocion promo) {
        if (promo.getDescripcion() != null && !promo.getDescripcion().isBlank()) {
            return promo.getDescripcion();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(promocionPorcentaje(promo.getPorcentaje())).append("% de descuento");
        if (promo.getClima() != null) {
            sb.append(" en plantas ").append(nombreClima(promo.getClima()));
        } else if (promo.getTipoAplicacion() == TipoAplicacion.CATEGORIA && promo.getCategoria() != null) {
            sb.append(" en ").append(promo.getCategoria().getNombre());
        } else if (promo.getTipoAplicacion() == TipoAplicacion.PLANTAS) {
            int cantidad = promo.getPlantas() == null ? 0 : promo.getPlantas().size();
            sb.append(" en ").append(cantidad).append(cantidad == 1 ? " producto" : " productos");
        }
        return sb.toString();
    }

    /**
     * Una planta solo puede pertenecer a un paquete de rebajas activo para que
     * el descuento aplicado sea siempre el del paquete al que pertenece.
     */
    private void validarPlantasEnUnSoloPaquete(Set<Plant> seleccionadas, Long promoId) {
        if (seleccionadas == null || seleccionadas.isEmpty()) {
            return;
        }
        Set<Long> idsSeleccionados = seleccionadas.stream()
                .map(Plant::getId)
                .collect(Collectors.toSet());
        List<String> conflictos = new ArrayList<>();

        for (Promocion otra : promocionRepository.findByActivaTrue()) {
            if (otra.getTipoAplicacion() != TipoAplicacion.PLANTAS
                    || (promoId != null && promoId.equals(otra.getId()))
                    || otra.getPlantas() == null) {
                continue;
            }
            for (Plant planta : otra.getPlantas()) {
                if (planta.getId() != null && idsSeleccionados.contains(planta.getId())) {
                    conflictos.add(planta.getNombreComun() + " (paquete \"" + otra.getNombre() + "\")");
                }
            }
        }

        if (!conflictos.isEmpty()) {
            throw new BusinessException("Una planta no puede estar en dos paquetes activos. "
                    + "Ya pertenece a otro paquete: " + String.join(", ", conflictos) + ".");
        }
    }

    /**
     * Un paquete de 2 o más productos se anuncia en la portada. Una rebaja
     * individual se aplica igual, pero no genera cartel de promoción.
     */
    private boolean esPromocionPublicable(Promocion promo) {
        return promo.getTipoAplicacion() != TipoAplicacion.PLANTAS
                || (promo.getPlantas() != null && promo.getPlantas().size() > 1);
    }

    private int promocionPorcentaje(BigDecimal pct) {
        return pct == null ? 0 : pct.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private String formatoPorcentaje(BigDecimal pct) {
        BigDecimal plano = pct == null ? BigDecimal.ZERO : pct.stripTrailingZeros();
        return plano.toPlainString();
    }

    private String nombreClima(TipoClima clima) {
        return clima == null ? "" : clima.name().substring(0, 1) + clima.name().substring(1).toLowerCase();
    }

    private Long extraerId(String slug) {
        try {
            return Long.valueOf(slug.substring("promocion-".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Umbral de envío gratis vigente. Fuente única: site_config (DB). Si la fila
     * no existe o el valor no es numérico, usa el de configuración.
     */
    private BigDecimal envioGratisDesde() {
        String valor = siteConfigService.obtener(CLAVE_GRATIS_DESDE,
                gratisDesdeRespaldo == null ? "0" : gratisDesdeRespaldo.toPlainString());
        try {
            return new BigDecimal(valor == null ? "0" : valor.trim());
        } catch (NumberFormatException e) {
            return gratisDesdeRespaldo == null ? BigDecimal.ZERO : gratisDesdeRespaldo;
        }
    }

    /** Formatea un monto en pesos argentinos: 90000 -> "90.000". */
    private String formatoMontoPesos(BigDecimal monto) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(monto == null ? BigDecimal.ZERO : monto);
    }
}