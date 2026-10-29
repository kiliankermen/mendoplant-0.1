package com.mendoplant.app.dto;

import com.mendoplant.app.model.Ambiente;
import com.mendoplant.app.model.NivelCuidado;
import com.mendoplant.app.model.Tamano;
import com.mendoplant.app.model.TipoAccesorio;
import com.mendoplant.app.model.TipoClima;
import com.mendoplant.app.model.TipoLuz;
import com.mendoplant.app.model.TipoMaceta;
import com.mendoplant.app.model.TipoPlanta;
import com.mendoplant.app.model.TipoSustrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Filtros del catálogo. Cada atributo es un conjunto: se pueden elegir varias
 * etiquetas de la misma categoría (por ejemplo Interior + Exterior), y se
 * muestran las plantas que tengan al menos una de ellas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantFilterDTO {
    private Long categoriaId;
    private Set<Ambiente> ambientes = new LinkedHashSet<>();
    private Set<TipoClima> climas = new LinkedHashSet<>();
    private Set<NivelCuidado> nivelesCuidado = new LinkedHashSet<>();
    private Set<TipoPlanta> tiposPlanta = new LinkedHashSet<>();
    private Set<TipoLuz> luces = new LinkedHashSet<>();
    private Set<Tamano> tamanos = new LinkedHashSet<>();
    private Set<TipoSustrato> tiposSustrato = new LinkedHashSet<>();
    private Set<TipoMaceta> tiposMaceta = new LinkedHashSet<>();
    private Set<TipoAccesorio> tiposAccesorio = new LinkedHashSet<>();
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private String texto;
    private String orden;
    private String modoFiltro; // INCLUYENTE (por defecto) o EXCLUYENTE
}
