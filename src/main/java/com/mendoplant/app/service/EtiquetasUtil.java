package com.mendoplant.app.service;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Etiquetas legibles en español para los atributos de una planta. */
@Component("etiquetasUtil")
public class EtiquetasUtil {

    /** Etiquetas de una planta, sin repetidas (se comparan sin acentos, sin mayúsculas y sin plural). */
    public List<String> etiquetas(Plant planta) {
        if (planta == null) {
            return List.of();
        }
        List<String> etiquetas = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();

        for (TipoPlanta tp : planta.getTiposPlanta()) {
            agregar(etiquetas, vistos, tipoPlanta(tp));
        }
        for (TipoClima cl : planta.getClimas()) {
            agregar(etiquetas, vistos, clima(cl));
        }
        for (TipoLuz l : planta.getLuces()) {
            agregar(etiquetas, vistos, luz(l));
        }
        for (NivelCuidado nc : planta.getNivelesCuidado()) {
            agregar(etiquetas, vistos, nivelCuidado(nc));
        }
        for (Tamano t : planta.getTamanos()) {
            agregar(etiquetas, vistos, tamano(t));
        }

        if (planta.getCategoria() != null) {
            String nombre = planta.getCategoria().getNombre();
            if (!"plantas".equalsIgnoreCase(nombre)
                    && !"interior".equalsIgnoreCase(nombre)
                    && !"exterior".equalsIgnoreCase(nombre)) {
                agregar(etiquetas, vistos, nombre);
            }
        }

        for (TipoMaceta m : planta.getTiposMaceta()) {
            agregar(etiquetas, vistos, tipoMaceta(m));
        }
        for (TipoSustrato s : planta.getTiposSustrato()) {
            agregar(etiquetas, vistos, tipoSustrato(s));
        }
        for (TipoAccesorio a : planta.getTiposAccesorio()) {
            agregar(etiquetas, vistos, tipoAccesorio(a));
        }

        return etiquetas;
    }

    /* Etiquetas legibles de un conjunto de valores (cada atributo admite varias). */

    public List<String> ambientes(Collection<Ambiente> valores) {
        return etiquetasDe(valores, this::ambiente);
    }

    public List<String> climas(Collection<TipoClima> valores) {
        return etiquetasDe(valores, this::clima);
    }

    public List<String> luces(Collection<TipoLuz> valores) {
        return etiquetasDe(valores, this::luz);
    }

    public List<String> nivelesCuidado(Collection<NivelCuidado> valores) {
        return etiquetasDe(valores, this::nivelCuidado);
    }

    public List<String> tiposPlanta(Collection<TipoPlanta> valores) {
        return etiquetasDe(valores, this::tipoPlanta);
    }

    public List<String> tamanos(Collection<Tamano> valores) {
        return etiquetasDe(valores, this::tamano);
    }

    public List<String> tiposMaceta(Collection<TipoMaceta> valores) {
        return etiquetasDe(valores, this::tipoMaceta);
    }

    public List<String> tiposSustrato(Collection<TipoSustrato> valores) {
        return etiquetasDe(valores, this::tipoSustrato);
    }

    public List<String> tiposAccesorio(Collection<TipoAccesorio> valores) {
        return etiquetasDe(valores, this::tipoAccesorio);
    }

    private <E> List<String> etiquetasDe(Collection<E> valores, Function<E, String> etiqueta) {
        if (valores == null || valores.isEmpty()) {
            return List.of();
        }
        List<String> salida = new ArrayList<>();
        for (E valor : valores) {
            String texto = etiqueta.apply(valor);
            if (texto != null && !texto.isBlank()) {
                salida.add(texto);
            }
        }
        return salida;
    }

    private void agregar(List<String> etiquetas, Set<String> vistos, String etiqueta) {
        if (etiqueta == null || etiqueta.isBlank()) {
            return;
        }
        if (vistos.add(clave(etiqueta))) {
            etiquetas.add(etiqueta);
        }
    }

    private String clave(String etiqueta) {
        String s = etiqueta.toLowerCase()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u');
        if (s.endsWith("s")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

public String ambiente(Ambiente valor) {
        if (valor == null) return "";
        return switch (valor) {
            case INTERIOR -> "Interior";
            case EXTERIOR -> "Exterior";
        };
    }

    public String tipoPlanta(TipoPlanta valor) {
        if (valor == null) return "";
        return switch (valor) {
            case SUCULENTA -> "Suculenta";
            case CACTUS -> "Cactus";
            case HELECHO -> "Helecho";
            case FLOR -> "Flor";
            case TREBOL -> "Trébol";
            case ARBOL -> "Árbol";
            case ARBUSTO -> "Arbusto";
            case ENREDADERA -> "Enredadera";
            case PALMERA -> "Palmera";
            case ORQUIDEA -> "Orquídea";
            case AROMATICA -> "Aromática";
            case TROPICAL -> "Tropical";
        };
    }

    public String clima(TipoClima valor) {
        if (valor == null) return "";
        return switch (valor) {
            case TEMPLADO -> "Templado";
            case TROPICAL -> "Tropical";
            case SECO -> "Árido";
            case HUMEDO -> "Húmedo";
            case FRIO -> "Frío";
        };
    }

    public String luz(TipoLuz valor) {
        if (valor == null) return "";
        return switch (valor) {
            case SOL_DIRECTO -> "Sol directo";
            case SOMBRA -> "Sombra";
            case MEDIA_SOMBRA -> "Media sombra";
        };
    }

    public String nivelCuidado(NivelCuidado valor) {
        if (valor == null) return "";
        return switch (valor) {
            case PRINCIPIANTE -> "Fácil";
            case INTERMEDIO -> "Moderado";
            case EXPERTO -> "Exigente";
        };
    }

    public String tamano(Tamano valor) {
        if (valor == null) return "";
        return switch (valor) {
            case PEQUENO -> "Pequeño";
            case MEDIANO -> "Mediano";
            case GRANDE -> "Grande";
            case GIGANTE -> "Gigante";
        };
    }

    public String tipoMaceta(TipoMaceta valor) {
        if (valor == null) return "";
        return switch (valor) {
            case PLASTICO -> "Maceta de plástico";
            case BARRO -> "Maceta de barro";
            case CERAMICA -> "Maceta de cerámica";
            case AUTORRIEGO -> "Maceta de autorriego";
        };
    }

    public String tipoSustrato(TipoSustrato valor) {
        if (valor == null) return "";
        return switch (valor) {
            case UNIVERSAL -> "Sustrato universal";
            case CACTUS_SUCULENTAS -> "Sustrato para cactus";
            case DRENAJE_ALTO -> "Drenaje alto";
            case TURBA_PERLITA -> "Turba y perlita";
        };
    }

    public String tipoAccesorio(TipoAccesorio valor) {
        if (valor == null) return "";
        return switch (valor) {
            case MESA -> "Mesa para macetas";
            case COLGANTE -> "Colgante";
            case ESTANTE -> "Estante";
            case TUTOR -> "Tutor";
        };
    }
}