package com.mendoplant.app.repository;

import com.mendoplant.app.model.Ambiente;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.TipoPlanta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long>, JpaSpecificationExecutor<Plant> {
    List<Plant> findByVendedorIdOrderByFechaPublicacionDesc(Long vendedorId);
    List<Plant> findTop8ByActivoTrueOrderByFechaPublicacionDesc();
    List<Plant> findByActivoTrueAndEsComboTrueOrderByFechaPublicacionDesc();

    Optional<Plant> findFirstByNombreCientificoIgnoreCase(String nombreCientifico);

    @Query("select p from Plant p where lower(replace(replace(p.nombreCientifico,' ','-'),'_','-')) = :slug")
    Optional<Plant> findFirstByNombreCientificoSlug(@Param("slug") String slug);

    /** Plantas de un ambiente (pueden tener varias etiquetas de ambiente). */
    @Query("select distinct p from Plant p join p.ambientes a "
            + "where p.activo = true and a = :ambiente order by p.fechaPublicacion desc")
    List<Plant> buscarPorAmbiente(@Param("ambiente") Ambiente ambiente, Pageable pageable);

    /** Plantas de un tipo de planta (pueden tener varias etiquetas de tipo). */
    @Query("select distinct p from Plant p join p.tiposPlanta t "
            + "where p.activo = true and t = :tipoPlanta order by p.fechaPublicacion desc")
    List<Plant> buscarPorTipoPlanta(@Param("tipoPlanta") TipoPlanta tipoPlanta, Pageable pageable);

    /**
     * Descuento atómico y condicional de stock (evita sobreventa bajo concurrencia):
     * solo resta si hay stock suficiente, dentro de un UPDATE de fila única (InnoDB).
     * Devuelve la cantidad de filas afectadas (1 = ok, 0 = stock insuficiente).
     */
    @Modifying
    @Query("update Plant p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad and p.activo = true")
    int descontarStockAtomico(@Param("id") Long id, @Param("cantidad") int cantidad);

    /** Reposición atómica de stock (cancelación de orden). */
    @Modifying
    @Query("update Plant p set p.stock = p.stock + :cantidad where p.id = :id")
    int reponerStockAtomico(@Param("id") Long id, @Param("cantidad") int cantidad);

    /** Incremento atómico del contador de ventas (pago aprobado). */
    @Modifying
    @Query("update Plant p set p.ventas = p.ventas + :cantidad where p.id = :id")
    int incrementarVentasAtomico(@Param("id") Long id, @Param("cantidad") int cantidad);
}
