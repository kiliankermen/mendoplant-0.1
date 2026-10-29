package com.mendoplant.app.service;

import com.mendoplant.app.model.Plant;

import java.util.Set;

public interface WishlistService {
    Set<Plant> listar(Long userId);
    void agregar(Long userId, Long plantId);
    void quitar(Long userId, Long plantId);
    boolean contiene(Long userId, Long plantId);
}
