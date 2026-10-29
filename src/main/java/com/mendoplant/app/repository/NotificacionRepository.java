package com.mendoplant.app.repository;

import com.mendoplant.app.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdAndEliminadaFalseOrderByFechaDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalseAndEliminadaFalse(Long usuarioId);

    boolean existsByUsuarioIdAndTitulo(Long usuarioId, String titulo);

    /** True si ya existe una notificación para ese usuario referida a esa planta. */
    boolean existsByUsuarioIdAndPlantaId(Long usuarioId, Long plantaId);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.usuario.id = :usuarioId and n.leida = false and n.eliminada = false")
    int marcarTodasComoLeidas(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("update Notificacion n set n.eliminada = true where n.id = :id and n.usuario.id = :usuarioId and n.eliminada = false")
    int eliminar(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("update Notificacion n set n.eliminada = true where n.usuario.id = :usuarioId and n.eliminada = false")
    int eliminarTodas(@Param("usuarioId") Long usuarioId);
}