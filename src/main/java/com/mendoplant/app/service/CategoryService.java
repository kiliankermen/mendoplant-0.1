package com.mendoplant.app.service;

import com.mendoplant.app.model.Category;

import java.util.List;

public interface CategoryService {
    List<Category> listarTodas();
    List<Category> listarActivas();
    Category buscarPorId(Long id);
    Category guardar(Category category);
    void eliminar(Long id);
}
