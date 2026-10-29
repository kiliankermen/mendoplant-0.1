package com.mendoplant.app.service;

import com.mendoplant.app.dto.PlantFilterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma el texto legible de los filtros aplicados para el cartel
 * "Filtros aplicados" del buscador (portada, catálogo, tienda).
 */
@Component
@RequiredArgsConstructor
public class FiltrosResumenUtil {

    private final CategoryService categoryService;
    private final EtiquetasUtil etiquetasUtil;

    public String resumen(PlantFilterDTO filtro) {
        if (filtro == null) {
            return "filtros aplicados";
        }
        List<String> partes = new ArrayList<>();
        if (filtro.getCategoriaId() != null) {
            categoryService.listarTodas().stream()
                    .filter(c -> c.getId().equals(filtro.getCategoriaId()))
                    .findFirst()
                    .ifPresentOrElse(
                            c -> partes.add(c.getNombre()),
                            () -> partes.add("Categoría " + filtro.getCategoriaId()));
        }
        if (filtro.getTexto() != null && !filtro.getTexto().isBlank()) {
            partes.add("Búsqueda: \"" + filtro.getTexto().trim() + "\"");
        }
        partes.addAll(etiquetasUtil.tiposPlanta(filtro.getTiposPlanta()));
        partes.addAll(etiquetasUtil.ambientes(filtro.getAmbientes()));
        partes.addAll(etiquetasUtil.climas(filtro.getClimas()));
        partes.addAll(etiquetasUtil.luces(filtro.getLuces()));
        for (String cuidado : etiquetasUtil.nivelesCuidado(filtro.getNivelesCuidado())) {
            partes.add("Cuidado " + cuidado.toLowerCase());
        }
        partes.addAll(etiquetasUtil.tamanos(filtro.getTamanos()));
        partes.addAll(etiquetasUtil.tiposSustrato(filtro.getTiposSustrato()));
        partes.addAll(etiquetasUtil.tiposMaceta(filtro.getTiposMaceta()));
        partes.addAll(etiquetasUtil.tiposAccesorio(filtro.getTiposAccesorio()));
        if (filtro.getPrecioMin() != null && filtro.getPrecioMax() != null) {
            partes.add("desde $" + filtro.getPrecioMin() + " hasta $" + filtro.getPrecioMax());
        } else if (filtro.getPrecioMin() != null) {
            partes.add("desde $" + filtro.getPrecioMin());
        } else if (filtro.getPrecioMax() != null) {
            partes.add("hasta $" + filtro.getPrecioMax());
        }
        return partes.isEmpty() ? "filtros aplicados" : String.join(", ", partes);
    }
}