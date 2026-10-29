package com.mendoplant.app.service.impl;

import com.mendoplant.app.dto.NotificacionDTO;
import com.mendoplant.app.dto.PromocionDTO;
import com.mendoplant.app.model.Notificacion;
import com.mendoplant.app.model.Plant;
import com.mendoplant.app.model.User;
import com.mendoplant.app.repository.NotificacionRepository;
import com.mendoplant.app.repository.UserRepository;
import com.mendoplant.app.service.NotificacionService;
import com.mendoplant.app.service.PromocionService;
import com.mendoplant.app.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UserRepository userRepository;
    private final PromocionService promocionService;

    @Override
    @Transactional
    public List<NotificacionDTO> notificacionesPara(Long usuarioId) {
        if (usuarioId == null) {
            return List.of();
        }
        asegurarNotificacionesBase(usuarioId);
        return notificacionRepository.findByUsuarioIdAndEliminadaFalseOrderByFechaDesc(usuarioId).stream()
                .map(this::aDto)
                .collect(Collectors.toList());
    }

    @Override
    public long noLeidas(Long usuarioId) {
        if (usuarioId == null) {
            return 0;
        }
        User usuario = userRepository.findById(usuarioId).orElse(null);
        if (usuario != null && usuario.isNotificacionesSilenciadas()) {
            return 0;
        }
        return notificacionRepository.countByUsuarioIdAndLeidaFalseAndEliminadaFalse(usuarioId);
    }

    @Override
    @Transactional
    public void marcarTodasComoLeidas(Long usuarioId) {
        if (usuarioId != null) {
            notificacionRepository.marcarTodasComoLeidas(usuarioId);
        }
    }

    @Override
    public boolean silenciadas(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        return userRepository.findById(usuarioId)
                .map(User::isNotificacionesSilenciadas)
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean alternarSilencio(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        User usuario = userRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return false;
        }
        usuario.setNotificacionesSilenciadas(!usuario.isNotificacionesSilenciadas());
        userRepository.save(usuario);
        return usuario.isNotificacionesSilenciadas();
    }

    @Override
    @Transactional
    public void eliminar(Long usuarioId, Long notificacionId) {
        if (usuarioId != null && notificacionId != null) {
            notificacionRepository.eliminar(notificacionId, usuarioId);
        }
    }

    @Override
    @Transactional
    public void eliminarTodas(Long usuarioId) {
        if (usuarioId != null) {
            notificacionRepository.eliminarTodas(usuarioId);
        }
    }

    @Override
    public void crear(Long usuarioId, String icono, String titulo, String mensaje) {
        crear(usuarioId, icono, titulo, mensaje, null);
    }

    /** Crea la notificación opcionalmente vinculada a una planta (p. ej. favorito con descuento). */
    @Override
    public void crear(Long usuarioId, String icono, String titulo, String mensaje, Plant planta) {
        if (usuarioId == null) {
            return;
        }
        User usuario = userRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return;
        }
        notificacionRepository.save(Notificacion.builder()
                .usuario(usuario)
                .planta(planta)
                .icono(icono)
                .titulo(titulo)
                .mensaje(mensaje)
                .fecha(java.time.LocalDateTime.now())
                .leida(false)
                .build());
    }

    /** Crea (una sola vez por usuario) los avisos de bienvenida, guías y promos activas. */
    private void asegurarNotificacionesBase(Long usuarioId) {
        if (!notificacionRepository.existsByUsuarioIdAndTitulo(usuarioId, "Bienvenido a Mendoplant")) {
            crear(usuarioId, "🌱", "Bienvenido a Mendoplant",
                    "Encontrá la planta ideal para cada rincón de tu casa.");
        }
        if (!notificacionRepository.existsByUsuarioIdAndTitulo(usuarioId, "Nuevas guías de cuidado")) {
            crear(usuarioId, "💧", "Nuevas guías de cuidado",
                    "Sumamos consejos de riego y mantenimiento en la sección Guías.");
        }
        for (PromocionDTO promo : promocionService.promocionesActivas()) {
            if (promo.isActiva()
                    && !notificacionRepository.existsByUsuarioIdAndTitulo(usuarioId, promo.getTitulo())) {
                crear(usuarioId, iconoPromo(promo.getColor()), promo.getTitulo(), promo.getSubtitulo());
            }
        }
    }

    private String iconoPromo(String color) {
        if (color == null) {
            return "🏷️";
        }
        return switch (color) {
            case "combo" -> "🎁";
            case "oferta" -> "🏷️";
            default -> "🌷";
        };
    }

    /**
     * Revisa diariamente los favoritos de cada usuario y avisa cuando una planta
     * tiene descuento visible en ese momento. Corre a las 00:05, justo después de
     * renovarse la oferta del día.
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void notificarDescuentosEnFavoritos() {
        List<User> usuarios = userRepository.findAll();
        for (User usuario : usuarios) {
            Set<Plant> favoritas = usuario.getListaDeseos();
            for (Plant planta : favoritas) {
                int pct = promocionService.porcentajeDescuentoVisible(planta);
                if (pct <= 0) {
                    continue;
                }
                if (notificacionRepository.existsByUsuarioIdAndPlantaId(usuario.getId(), planta.getId())) {
                    continue;
                }
                String nombre = planta.getNombreComun() != null ? planta.getNombreComun() : "tu favorita";
                crear(usuario.getId(), "🏷️",
                        "Descuento en " + nombre,
                        "-" + pct + "% en tu favorita " + nombre + ". ¡Aprovechá antes de que vuelva a su precio!",
                        planta);
            }
        }
    }

    private NotificacionDTO aDto(Notificacion n) {
        return NotificacionDTO.builder()
                .id(n.getId())
                .icono(n.getIcono())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .fecha(n.getFecha())
                .leida(n.isLeida())
                .plantaId(n.getPlanta() != null ? n.getPlanta().getId() : null)
                .build();
    }
}