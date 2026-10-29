package com.mendoplant.app.service.impl;

import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.User;
import com.mendoplant.app.repository.PlantRepository;
import com.mendoplant.app.repository.UserRepository;
import com.mendoplant.app.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final UserRepository userRepository;
    private final PlantRepository plantRepository;

    @Override
    public Set<Plant> listar(Long userId) {
        return obtenerUsuario(userId).getListaDeseos();
    }

    @Override
    @Transactional
    public void agregar(Long userId, Long plantId) {
        User user = obtenerUsuario(userId);
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResourceNotFoundException("Planta no encontrada con id: " + plantId));
        user.getListaDeseos().add(plant);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void quitar(Long userId, Long plantId) {
        User user = obtenerUsuario(userId);
        user.getListaDeseos().removeIf(p -> p.getId().equals(plantId));
        userRepository.save(user);
    }

    @Override
    public boolean contiene(Long userId, Long plantId) {
        return obtenerUsuario(userId).getListaDeseos().stream()
                .anyMatch(p -> p.getId().equals(plantId));
    }

    private User obtenerUsuario(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));
    }
}
