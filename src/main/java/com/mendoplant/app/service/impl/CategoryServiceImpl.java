package com.mendoplant.app.service.impl;

import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.Category;
import com.mendoplant.app.repository.CategoryRepository;
import com.mendoplant.app.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> listarTodas() {
        return categoryRepository.findAll();
    }

    /** Categorías vigentes (no eliminadas lógicamente), ordenadas por nombre. */
    @Override
    public List<Category> listarActivas() {
        return categoryRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Override
    public Category buscarPorId(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
    }

    @Override
    @Transactional
    public Category guardar(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Category categoria = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
        // Eliminación lógica: las plantas y promociones asociadas conservan la FK.
        categoria.setActivo(false);
        categoryRepository.save(categoria);
    }
}
