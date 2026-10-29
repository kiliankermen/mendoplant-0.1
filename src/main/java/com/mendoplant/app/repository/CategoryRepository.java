package com.mendoplant.app.repository;

import com.mendoplant.app.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Categorías vigentes (no eliminadas lógicamente), en orden alfabético. */
    List<Category> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}
