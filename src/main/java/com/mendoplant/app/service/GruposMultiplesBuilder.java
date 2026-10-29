package com.mendoplant.app.service;

import com.mendoplant.app.dto.GrupoMulti;
import com.mendoplant.app.dto.OpcionMulti;
import com.mendoplant.app.dto.PlantDTO;
import com.mendoplant.app.dto.PlantFilterDTO;
import com.mendoplant.app.model.Ambiente;
import com.mendoplant.app.model.NivelCuidado;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.Tamano;
import com.mendoplant.app.model.TipoAccesorio;
import com.mendoplant.app.model.TipoClima;
import com.mendoplant.app.model.TipoLuz;
import com.mendoplant.app.model.TipoMaceta;
import com.mendoplant.app.model.TipoPlanta;
import com.mendoplant.app.model.TipoSustrato;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Arma los grupos de opciones de los desplegables con casillas de verificación
 * (formulario de publicación y panel de filtros de la portada).
 * Cada opción lleva el valor crudo del enum y la etiqueta legible en español.
 */
@Component("gruposMultiplesBuilder")
public class GruposMultiplesBuilder {

    private final EtiquetasUtil etiquetasUtil;

    public GruposMultiplesBuilder(EtiquetasUtil etiquetasUtil) {
        this.etiquetasUtil = etiquetasUtil;
    }

    /** Grupos del formulario de publicación/edición, con lo que ya tiene la planta. */
    public List<GrupoMulti> gruposFormulario(PlantDTO dto) {
        return List.of(
                grupo("tamanos", "Tamaño", Tamano.values(), dto.getTamanos(), etiquetasUtil::tamano),
                grupo("luces", "Luz", TipoLuz.values(), dto.getLuces(), etiquetasUtil::luz),
                grupo("tiposMaceta", "Maceta recomendada", TipoMaceta.values(), dto.getTiposMaceta(), etiquetasUtil::tipoMaceta),
                grupo("tiposSustrato", "Sustrato ideal", TipoSustrato.values(), dto.getTiposSustrato(), etiquetasUtil::tipoSustrato),
                grupo("tiposAccesorio", "Tipo de accesorio", TipoAccesorio.values(), dto.getTiposAccesorio(), etiquetasUtil::tipoAccesorio),
                grupo("ambientes", "Ambiente", Ambiente.values(), dto.getAmbientes(), etiquetasUtil::ambiente),
                grupo("climas", "Clima", TipoClima.values(), dto.getClimas(), etiquetasUtil::clima),
                grupo("nivelesCuidado", "Nivel de cuidado", NivelCuidado.values(), dto.getNivelesCuidado(), etiquetasUtil::nivelCuidado),
                grupo("tiposPlanta", "Tipo de planta", TipoPlanta.values(), dto.getTiposPlanta(), etiquetasUtil::tipoPlanta)
        );
    }

    /**
     * Grupos del formulario de edición con lo que tiene la planta guardada.
     * Se usa la entidad y no el DTO porque el DTO se enlaza desde el formulario
     * y sus conjuntos se reemplazan al recibir la respuesta.
     */
    public List<GrupoMulti> gruposFormulario(Plant planta) {
        return List.of(
                grupo("tamanos", "Tamaño", Tamano.values(), planta.getTamanos(), etiquetasUtil::tamano),
                grupo("luces", "Luz", TipoLuz.values(), planta.getLuces(), etiquetasUtil::luz),
                grupo("tiposMaceta", "Maceta recomendada", TipoMaceta.values(), planta.getTiposMaceta(), etiquetasUtil::tipoMaceta),
                grupo("tiposSustrato", "Sustrato ideal", TipoSustrato.values(), planta.getTiposSustrato(), etiquetasUtil::tipoSustrato),
                grupo("tiposAccesorio", "Tipo de accesorio", TipoAccesorio.values(), planta.getTiposAccesorio(), etiquetasUtil::tipoAccesorio),
                grupo("ambientes", "Ambiente", Ambiente.values(), planta.getAmbientes(), etiquetasUtil::ambiente),
                grupo("climas", "Clima", TipoClima.values(), planta.getClimas(), etiquetasUtil::clima),
                grupo("nivelesCuidado", "Nivel de cuidado", NivelCuidado.values(), planta.getNivelesCuidado(), etiquetasUtil::nivelCuidado),
                grupo("tiposPlanta", "Tipo de planta", TipoPlanta.values(), planta.getTiposPlanta(), etiquetasUtil::tipoPlanta)
        );
    }

    /** Grupos del panel de filtros de la portada, con los filtros ya aplicados. */
    public List<GrupoMulti> gruposFiltro(PlantFilterDTO filtro) {
        return List.of(
                grupo("ambientes", "Tipo de ambiente", Ambiente.values(), filtro.getAmbientes(), etiquetasUtil::ambiente),
                grupo("climas", "Tipo de clima", TipoClima.values(), filtro.getClimas(), etiquetasUtil::clima),
                grupo("nivelesCuidado", "Tipo de cuidado", NivelCuidado.values(), filtro.getNivelesCuidado(), etiquetasUtil::nivelCuidado),
                grupo("tiposPlanta", "Tipo de planta", TipoPlanta.values(), filtro.getTiposPlanta(), etiquetasUtil::tipoPlanta),
                grupo("luces", "Luz", TipoLuz.values(), filtro.getLuces(), etiquetasUtil::luz),
                grupo("tamanos", "Tamaño", Tamano.values(), filtro.getTamanos(), etiquetasUtil::tamano)
        );
    }

    public <E extends Enum<E>> GrupoMulti grupo(String campo, String titulo, E[] valores,
                                                 Collection<E> seleccionados, Function<E, String> etiqueta) {
        List<OpcionMulti> opciones = new ArrayList<>();
        for (E valor : valores) {
            opciones.add(new OpcionMulti(valor.name(), etiqueta.apply(valor),
                    seleccionados != null && seleccionados.contains(valor)));
        }
        return new GrupoMulti(campo, titulo, opciones);
    }
}
