package com.mendoplant.app.service.impl;

import com.mendoplant.app.dto.CarritoItemDTO;
import com.mendoplant.app.model.CarritoItem;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.User;
import com.mendoplant.app.repository.CarritoItemRepository;
import com.mendoplant.app.repository.UserRepository;
import com.mendoplant.app.service.CarritoService;
import com.mendoplant.app.service.PlantService;
import com.mendoplant.app.service.PromocionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrito de compras de la sesión HTTP, respaldado en la base de datos:
 * cada vez que un usuario autenticado modifica su carrito, los ítems se
 * persisten en {@code carrito_item}; al crear una sesión nueva se recargan,
 * así no se pierde el carrito armado al cerrar sesión.
 */
@Service
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class CarritoServiceImpl implements CarritoService {

    private final Map<Long, Integer> items = new LinkedHashMap<>();

    private final PlantService plantService;
    private final PromocionService promocionService;
    private final CarritoItemRepository carritoItemRepository;
    private final UserRepository userRepository;

    /** Email del usuario cuyo carrito de BD ya se cargó/mapeó en esta sesión. */
    private String cargadoPara = null;

    @Override
    public List<CarritoItemDTO> items() {
        asegurarCargaDb();
        List<CarritoItemDTO> resultado = new ArrayList<>();
        List<Long> invalidos = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            try {
                Plant plant = plantService.buscarPorId(entry.getKey());
                if (plant == null || !plant.isActivo()) {
                    invalidos.add(entry.getKey());
                    continue;
                }
                Integer cantidad = entry.getValue();
                BigDecimal precioDescontado = promocionService.tieneDescuento(plant)
                        ? promocionService.precioConDescuento(plant)
                        : null;
                BigDecimal precioUnitario = plant.getPrecio();
                BigDecimal unit = precioDescontado != null ? precioDescontado : precioUnitario;
                resultado.add(CarritoItemDTO.builder()
                        .plantId(plant.getId())
                        .nombre(plant.getNombreComun())
                        .plantaNombreCientifico(plant.getNombreCientifico())
                        .imagenUrl(plant.getImagenUrl())
                        .precioUnitario(precioUnitario)
                        .precioDescontado(precioDescontado)
                        .cantidad(cantidad)
                        .subtotal(unit.multiply(BigDecimal.valueOf(cantidad)))
                        .build());
            } catch (Exception e) {
                invalidos.add(entry.getKey());
            }
        }
        if (!invalidos.isEmpty()) {
            for (Long id : invalidos) {
                items.remove(id);
            }
            sincronizarDb();
        }
        return resultado;
    }

    @Override
    @Transactional
    public void agregar(Long plantId, int cantidad) {
        if (cantidad <= 0) {
            return;
        }
        items.merge(plantId, cantidad, Integer::sum);
        sincronizarDb();
    }

    @Override
    @Transactional
    public void quitar(Long plantId) {
        asegurarCargaDb();
        items.remove(plantId);
        sincronizarDb();
    }

    @Override
    @Transactional
    public void restar(Long plantId) {
        asegurarCargaDb();
        Integer actual = items.get(plantId);
        if (actual == null) {
            return;
        }
        // El mínimo es 1: el botón "-" nunca elimina el ítem (para eso está quitar)
        if (actual > 1) {
            items.put(plantId, actual - 1);
        }
        sincronizarDb();
    }

    @Override
    @Transactional
    public void limpiar() {
        asegurarCargaDb();
        items.clear();
        sincronizarDb();
    }

    @Override
    public int cantidadItems() {
        asegurarCargaDb();
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public BigDecimal total() {
        return items().stream()
                .map(CarritoItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean vacio() {
        asegurarCargaDb();
        return items.isEmpty();
    }

    /**
     * Carga el carrito guardado en BD del usuario actual (solo la primera vez
     * por sesión y si el carrito en memoria está vacío).
     */
    private void asegurarCargaDb() {
        if (cargadoPara != null) {
            return;
        }
        User user = usuarioActual();
        if (user == null) {
            return;
        }
        if (items.isEmpty()) {
            for (CarritoItem ci : carritoItemRepository.findByUsuarioId(user.getId())) {
                items.merge(ci.getPlanta().getId(), ci.getCantidad(), Integer::sum);
            }
        }
        cargadoPara = user.getEmail();
    }

    /**
     * Persiste el estado actual del carrito en BD para el usuario autenticado:
     * reemplaza los ítems guardados por los de la sesión.
     */
    @Transactional
    protected void sincronizarDb() {
        User user = usuarioActual();
        if (user == null) {
            return;
        }
        cargadoPara = user.getEmail();
        List<CarritoItem> guardados = carritoItemRepository.findByUsuarioId(user.getId());
        carritoItemRepository.deleteAll(guardados);
        carritoItemRepository.flush();
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            carritoItemRepository.save(CarritoItem.builder()
                    .usuario(user)
                    .planta(Plant.builder().id(entry.getKey()).build())
                    .cantidad(entry.getValue())
                    .build());
        }
        carritoItemRepository.flush();
    }

    private User usuarioActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return null;
            }
            String email = auth.getName();
            if (email == null || email.isBlank()) {
                return null;
            }
            return userRepository.findByEmail(email).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}