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
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantDTO {

    private Long id;

    @NotBlank(message = "El nombre común es obligatorio")
    @Size(max = 120)
    private String nombreComun;

    @Size(max = 150)
    private String nombreCientifico;

    @Size(max = 2000)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    private String imagenUrl;

    /** Galería del producto (mínimo 2 al publicar desde el dashboard). */
    private List<String> imagenes = new ArrayList<>();

    /**
     * Categoría en la portada. LA ASIGNA SOLO EL ADMINISTRADOR desde
     * /admin/plantas (las plantas destacadas de la portada). El vendedor
     * no la elige: puede quedar null (aparece solo en el catálogo).
     */
    private Long categoriaId;

    /* Cada atributo admite varias etiquetas a la vez. */

    @NotEmpty(message = "Debés seleccionar al menos un tamaño")
    private Set<Tamano> tamanos = new LinkedHashSet<>();

    @NotEmpty(message = "Debés indicar al menos un requerimiento de luz")
    private Set<TipoLuz> luces = new LinkedHashSet<>();

    private Set<TipoMaceta> tiposMaceta = new LinkedHashSet<>();

    private Set<TipoSustrato> tiposSustrato = new LinkedHashSet<>();

    private Set<TipoAccesorio> tiposAccesorio = new LinkedHashSet<>();

    private Set<Ambiente> ambientes = new LinkedHashSet<>();

    private Set<TipoClima> climas = new LinkedHashSet<>();

    private Set<NivelCuidado> nivelesCuidado = new LinkedHashSet<>();

    private Set<TipoPlanta> tiposPlanta = new LinkedHashSet<>();

    private boolean esCombo;
}
