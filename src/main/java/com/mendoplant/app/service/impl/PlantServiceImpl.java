package com.mendoplant.app.service.impl;

import com.mendoplant.app.dto.PlantDTO;
import com.mendoplant.app.dto.PlantFilterDTO;
import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.Ambiente;
import com.mendoplant.app.model.Category;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.TipoPlanta;
import com.mendoplant.app.model.User;
import com.mendoplant.app.repository.CategoryRepository;
import com.mendoplant.app.repository.PlantRepository;
import com.mendoplant.app.repository.UserRepository;
import com.mendoplant.app.service.PlantService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlantServiceImpl implements PlantService {

    private final PlantRepository plantRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<Plant> buscarConFiltros(PlantFilterDTO filtro) {
        return plantRepository.findAll(construirSpec(filtro));
    }

    @Override
    public List<Plant> buscarCatalogoOrdenado(PlantFilterDTO filtro, String orden) {
        return plantRepository.findAll(construirSpec(filtro), sortPor(orden));
    }

    /** Traduce el parámetro de orden recibido por URL a un Sort de JPA. */
    private Sort sortPor(String orden) {
        if (orden == null || orden.isBlank()) {
            return Sort.by("ventas").descending();
        }
        return switch (orden) {
            case "precio-asc" -> Sort.by("precio").ascending();
            case "precio-desc" -> Sort.by("precio").descending();
            case "ventas-asc" -> Sort.by("ventas").ascending();
            case "nombre-asc" -> Sort.by("nombreComun").ascending();
            case "nombre-desc" -> Sort.by("nombreComun").descending();
            default -> Sort.by("ventas").descending();
        };
    }

    private Specification<Plant> construirSpec(PlantFilterDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("activo")));
            boolean conJoin = false;

            if (filtro != null) {
                if (filtro.getCategoriaId() != null) {
                    predicates.add(cb.equal(root.get("categoria").get("id"), filtro.getCategoriaId()));
                }
                String modo = filtro.getModoFiltro();
                boolean excluyente = modo != null && "EXCLUYENTE".equalsIgnoreCase(modo);
                // Dentro de una misma categoría los valores se acumulan (OR) por defecto (incluyente).
                // En modo excluyente, para los valores seleccionados de un atributo deben cumplirse TODOS (AND).
                conJoin |= agregarFiltro(root, cb, predicates, "ambientes", filtro.getAmbientes(), excluyente);
                conJoin |= agregarFiltro(root, cb, predicates, "climas", filtro.getClimas(), excluyente);
                conJoin |= agregarFiltro(root, cb, predicates, "nivelesCuidado", filtro.getNivelesCuidado(), excluyente);
                conJoin |= agregarFiltro(root, cb, predicates, "tiposPlanta", filtro.getTiposPlanta(), excluyente);
                conJoin |= agregarFiltro(root, cb, predicates, "luces", filtro.getLuces(), excluyente);
                conJoin |= agregarFiltro(root, cb, predicates, "tamanos", filtro.getTamanos(), excluyente);
                conJoin |= agregarFiltro(root, cb, predicates, "tiposSustrato", filtro.getTiposSustrato(), excluyente);
                conJoin |= agregarFiltro(root, cb, predicates, "tiposMaceta", filtro.getTiposMaceta(), excluyente);
                conJoin |= agregarFiltro(root, cb, predicates, "tiposAccesorio", filtro.getTiposAccesorio(), excluyente);
                if (filtro.getPrecioMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("precio"), filtro.getPrecioMin()));
                }
                if (filtro.getPrecioMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("precio"), filtro.getPrecioMax()));
                }
                if (filtro.getTexto() != null && !filtro.getTexto().isBlank()) {
                    String texto = filtro.getTexto().trim().toLowerCase();
                    String like = "%" + texto + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("nombreComun")), like),
                            cb.like(cb.lower(root.get("nombreCientifico")), like)
                    ));
                }
            }
            if (conJoin && query != null) {
                // Los joins pueden duplicar filas: se deduplican con DISTINCT.
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Agrega el filtro de un atributo multivalor (join + IN). Devuelve true si aplicó. */
    private <E extends Enum<E>> boolean agregarFiltro(Root<Plant> root, CriteriaBuilder cb,
                                                      List<Predicate> predicates, String atributo,
                                                      Set<E> valores) {
        return agregarFiltro(root, cb, predicates, atributo, valores, false);
    }

    private <E extends Enum<E>> boolean agregarFiltro(Root<Plant> root, CriteriaBuilder cb,
                                                      List<Predicate> predicates, String atributo,
                                                      Set<E> valores, boolean todosRequeridos) {
        Set<E> limpios = limpiar(valores);
        if (limpios.isEmpty()) {
            return false;
        }
        if (!todosRequeridos) {
            Join<Plant, E> join = root.join(atributo);
            predicates.add(join.in(limpios));
            return true;
        }
        // Modo excluyente (AND): la planta debe tener TODOS los valores seleccionados para ese atributo.
        for (E valor : limpios) {
            Join<Plant, E> join = root.join(atributo);
            predicates.add(cb.equal(join, valor));
        }
        return true;
    }

    /** Copia un conjunto de valores descartando los nulos (por ejemplo, un parámetro vacío). */
    public static <E> Set<E> limpiar(Set<E> valores) {
        Set<E> salida = new LinkedHashSet<>();
        if (valores != null) {
            for (E valor : valores) {
                if (valor != null) {
                    salida.add(valor);
                }
            }
        }
        return salida;
    }

    @Override
    public List<Plant> destacadas() {
        return plantRepository.findTop8ByActivoTrueOrderByFechaPublicacionDesc();
    }

    @Override
    public List<Plant> buscarPorAmbiente(Ambiente ambiente) {
        return plantRepository.buscarPorAmbiente(ambiente, PageRequest.of(0, 10));
    }

    @Override
    public List<Plant> buscarPorTipoPlanta(TipoPlanta tipoPlanta) {
        return plantRepository.buscarPorTipoPlanta(tipoPlanta, PageRequest.of(0, 10));
    }

    @Override
    public List<Plant> buscarCombos() {
        return plantRepository.findByActivoTrueAndEsComboTrueOrderByFechaPublicacionDesc();
    }

    @Override
    public Plant buscarPorId(Long id) {
        return plantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Planta no encontrada"));
    }

    @Override
    public Plant buscarPorNombreCientifico(String nombreCientifico) {
        if (nombreCientifico == null || nombreCientifico.isBlank()) {
            return null;
        }
        return plantRepository.findFirstByNombreCientificoIgnoreCase(nombreCientifico).orElse(null);
    }

    @Override
    public Plant buscarPorNombreCientificoSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return plantRepository.findFirstByNombreCientificoSlug(slug).orElse(null);
    }

    @Override
    public List<Plant> buscarPorVendedor(Long vendedorId) {
        return plantRepository.findByVendedorIdOrderByFechaPublicacionDesc(vendedorId);
    }

    @Override
    @Transactional
    public Plant publicar(PlantDTO dto, Long vendedorId) {
        User vendedor = userRepository.findById(vendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + vendedorId));

        Plant plant = Plant.builder()
                .nombreComun(dto.getNombreComun())
                .nombreCientifico(dto.getNombreCientifico())
                .descripcion(dto.getDescripcion())
                .precio(dto.getPrecio())
                .stock(dto.getStock())
                .imagenUrl(dto.getImagenUrl())
                .imagenes(dto.getImagenes() == null ? new ArrayList<>() : new ArrayList<>(dto.getImagenes()))
                .categoria(resolverCategoria(dto.getCategoriaId()))
                .tamanos(limpiar(dto.getTamanos()))
                .luces(limpiar(dto.getLuces()))
                .tiposMaceta(limpiar(dto.getTiposMaceta()))
                .tiposSustrato(limpiar(dto.getTiposSustrato()))
                .tiposAccesorio(limpiar(dto.getTiposAccesorio()))
                .ambientes(limpiar(dto.getAmbientes()))
                .climas(limpiar(dto.getClimas()))
                .nivelesCuidado(limpiar(dto.getNivelesCuidado()))
                .tiposPlanta(limpiar(dto.getTiposPlanta()))
                .esCombo(dto.isEsCombo())
                .vendedor(vendedor)
                .activo(true)
                .build();
        return plantRepository.save(plant);
    }

    @Override
    @Transactional
    public Plant actualizar(Long id, PlantDTO dto, Long vendedorId) {
        Plant plant = buscarPorId(id);
        verificarPropietario(plant, vendedorId);

        plant.setNombreComun(dto.getNombreComun());
        plant.setNombreCientifico(dto.getNombreCientifico());
        plant.setDescripcion(dto.getDescripcion());
        plant.setPrecio(dto.getPrecio());
        plant.setStock(dto.getStock());
        plant.setImagenUrl(dto.getImagenUrl());
        if (dto.getImagenes() != null && !dto.getImagenes().isEmpty()) {
            plant.setImagenes(new ArrayList<>(dto.getImagenes()));
        }
        // El vendedor no elige categoría: si el DTO no trae una (null), se conserva la actual.
        if (dto.getCategoriaId() != null) {
            plant.setCategoria(resolverCategoria(dto.getCategoriaId()));
        }
        plant.setTamanos(limpiar(dto.getTamanos()));
        plant.setLuces(limpiar(dto.getLuces()));
        plant.setTiposMaceta(limpiar(dto.getTiposMaceta()));
        plant.setTiposSustrato(limpiar(dto.getTiposSustrato()));
        plant.setTiposAccesorio(limpiar(dto.getTiposAccesorio()));
        plant.setAmbientes(limpiar(dto.getAmbientes()));
        plant.setClimas(limpiar(dto.getClimas()));
        plant.setNivelesCuidado(limpiar(dto.getNivelesCuidado()));
        plant.setTiposPlanta(limpiar(dto.getTiposPlanta()));
        plant.setEsCombo(dto.isEsCombo());
        return plantRepository.save(plant);
    }

    @Override
    @Transactional
    public void cambiarCategoria(Long plantId, Long categoriaId) {
        Plant plant = buscarPorId(plantId);
        plant.setCategoria(resolverCategoria(categoriaId));
        plantRepository.save(plant);
    }

    /** Devuelve la categoría o null si no se seleccionó (las plantas sin categoría no van a la portada). */
    private Category resolverCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categoryRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + categoriaId));
    }

    @Override
    @Transactional
    public void eliminar(Long id, Long vendedorId) {
        Plant plant = buscarPorId(id);
        verificarPropietario(plant, vendedorId);
        plant.setActivo(false);
        plantRepository.save(plant);
    }

    @Override
    @Transactional
    public void descontarStock(Long plantId, int cantidad) {
        int afectadas = plantRepository.descontarStockAtomico(plantId, cantidad);
        if (afectadas == 0) {
            Plant plant = buscarPorId(plantId);
            throw new BusinessException("Stock insuficiente para \"" + plant.getNombreComun()
                    + "\". Disponible: " + plant.getStock());
        }
    }

    @Override
    @Transactional
    public void reponerStock(Long plantId, int cantidad) {
        plantRepository.reponerStockAtomico(plantId, cantidad);
    }

    @Override
    @Transactional
    public void sumarVentas(Long plantId, int cantidad) {
        plantRepository.incrementarVentasAtomico(plantId, cantidad);
    }

    /** Un usuario común solo puede editar/eliminar sus propias publicaciones; un admin puede sobre cualquiera. */
    private void verificarPropietario(Plant plant, Long vendedorId) {
        User actor = userRepository.findById(vendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + vendedorId));
        boolean esAdmin = actor.getRole().name().equals("ROLE_ADMIN");
        boolean esDueño = plant.getVendedor() != null && plant.getVendedor().getId().equals(vendedorId);
        if (!esAdmin && !esDueño) {
            throw new BusinessException("No tenés permiso para modificar esta publicación");
        }
    }
}
