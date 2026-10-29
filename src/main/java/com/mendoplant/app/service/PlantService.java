package com.mendoplant.app.service;

import com.mendoplant.app.dto.PlantDTO;
import com.mendoplant.app.dto.PlantFilterDTO;
import com.mendoplant.app.model.Ambiente;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.TipoPlanta;

import java.util.List;

public interface PlantService {
    List<Plant> buscarConFiltros(PlantFilterDTO filtro);
    /**
     * Catálogo: misma búsqueda con filtros que {@link #buscarConFiltros}, ordenada
     * según el parámetro {@code orden}: precio-asc / precio-desc / ventas-desc /
     * ventas-asc / nombre-asc / nombre-desc (por defecto: más vendidas primero).
     * El orden alfabético usa el nombre común.
     */
    List<Plant> buscarCatalogoOrdenado(PlantFilterDTO filtro, String orden);
    List<Plant> destacadas();
    List<Plant> buscarPorAmbiente(Ambiente ambiente);
    List<Plant> buscarPorTipoPlanta(TipoPlanta tipoPlanta);
    List<Plant> buscarCombos();
    Plant buscarPorId(Long id);

    Plant buscarPorNombreCientifico(String nombreCientifico);

    Plant buscarPorNombreCientificoSlug(String slug);

    List<Plant> buscarPorVendedor(Long vendedorId);
    Plant publicar(PlantDTO dto, Long vendedorId);
    Plant actualizar(Long id, PlantDTO dto, Long vendedorId);
    void eliminar(Long id, Long vendedorId);
    /** Asigna (o quita, si categoriaId es null) la categoría de portada de una planta. Solo admin. */
    void cambiarCategoria(Long plantId, Long categoriaId);
    void descontarStock(Long plantId, int cantidad);

    void reponerStock(Long plantId, int cantidad);

    /** Suma unidades al contador de ventas de una planta (pago aprobado). */
    void sumarVentas(Long plantId, int cantidad);
}