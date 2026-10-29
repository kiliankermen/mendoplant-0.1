package com.mendoplant.app.service;

import com.mendoplant.app.dto.PromocionDTO;
import com.mendoplant.app.model.OfertaDia;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.Promocion;
import com.mendoplant.app.model.Plant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Sistema de descuentos y promociones, configurables desde el panel de ADMIN.
 * <p>
 * Fuente única de verdad de precios con descuento: carrito, checkout y órdenes
 * usan {@link #porcentajeDescuento} / {@link #precioConDescuento} para que el
 * precio cobrado sea SIEMPRE consistente con lo que ve el comprador.
 * <ul>
 *   <li><b>Descuentos por porcentaje</b>: promociones (percentual) administradas
 *       en {@literal promociones}, con vigencia por fechas y/o días de la semana.</li>
 *   <li><b>Oferta del día rotativa</b>: una planta destacada por día de la semana
 *       (ISO 1=lunes..7=domingo) con su % de descuento, en {@literal ofertas_dia}.</li>
 *   <li>Nunca se acumulan descuentos: se aplica el mayor porcentaje vigente.</li>
 * </ul>
 * El planeo del INSTANTE El lugar anterior hardcodeado "30% de primavera en
 * tropicales" hoy es una {@link Promocion} de base de datos (V2), por lo que
 * también es editable desde admin.
 */
public interface PromocionService {

    /** True durante la primavera argentina (septiembre-noviembre). Solo informativo. */
    boolean isPrimavera();

    /** Porcentaje de descuento vigente HOY para la planta (0 si no aplica ninguno). */
    BigDecimal porcentajeDescuento(Plant plant);

    /** Porcentaje de descuento como entero (para mostrar "-30%" sin decimales). */
    int porcentajeDescuentoVisible(Plant plant);

    /** True si la planta tiene algún descuento vigente hoy. */
    boolean tieneDescuento(Plant plant);

    /** Precio final con el mejor descuento vigente (sin descuento = precio de lista). */
    BigDecimal precioConDescuento(Plant plant);

    /** True si la planta es la de la Oferta del día vigente. */
    boolean esOfertaDelDia(Plant plant);

    /** Plantas de la Oferta del día vigente (una por día; vacío si no hay configurada). */
    List<Plant> ofertasDelDia();

    /** Carteles de promociones activas para la portada. */
    List<PromocionDTO> promocionesActivas();

    /** Filtra una lista de plantas por una promoción dada (slug de banner). */
    List<Plant> filtrarPorPromo(String slug, List<Plant> plantas);

    /** Título visible de una promo (para el resumen de filtros). */
    String tituloPromo(String slug);

    // ------------------------------------------------------------------
    // Gestión ADMIN
    // ------------------------------------------------------------------
    List<Promocion> listarPromociones();

    Promocion buscarPorId(Long id);

    Promocion guardarPromocion(Promocion promocion);

    /** Eliminación lógica: marca la promoción como inactiva. */
    void desactivarPromocion(Long id);

    List<OfertaDia> listarOfertasDia();

    /** Guarda (crea o actualiza) la oferta del día para un día de semana ISO. */
    OfertaDia guardarOfertaDia(int diaSemana, Long plantaId, BigDecimal porcentaje, boolean activa);

    /** Rebajas activas de un vendedor (paquetes e individuales). */
    List<Promocion> listarRebajasVendedor(String email);

    /** Busca una rebaja del vendedor validando la propiedad por email. */
    Promocion buscarRebajaVendedor(Long id, String email);

    /** Crea o actualiza un paquete/rebaja de un vendedor sobre un conjunto de productos. */
    Promocion guardarRebaja(Long id, String nombre, String descripcion, BigDecimal porcentaje,
                            String colorBanner, Set<Plant> plantas, String email);

    /** IDs de plantas que ya pertenecen a un paquete activo (excluyendo uno opcional). */
    Set<Long> plantasEnPaquetesActivos(Long excluirPromoId);

    /** Actualiza solo el porcentaje de una rebaja activa del vendedor. */
    Promocion actualizarPorcentajeRebaja(Long id, BigDecimal porcentaje, String email);

    /** Cambia el nombre, la descripción y el color de banner de una rebaja/paquete. */
    Promocion actualizarDatosRebaja(Long id, String nombre, String descripcion,
                                   String colorBanner, String email);

    /** Agrega una planta a una rebaja/paquete activo del vendedor. */
    Promocion agregarPlantaRebaja(Long id, Plant planta, String email);

    /** Quita una planta de una rebaja/paquete del vendedor. */
    Promocion quitarPlantaRebaja(Long id, Long plantaId, String email);

    /** Elimina (desactiva) un paquete/rebaja del vendedor. */
    void eliminarRebaja(Long id, String email);
}