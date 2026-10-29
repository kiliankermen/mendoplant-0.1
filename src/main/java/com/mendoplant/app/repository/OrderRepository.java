package com.mendoplant.app.repository;

import com.mendoplant.app.model.EstadoOrden;
import com.mendoplant.app.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCompradorIdOrderByFechaDesc(Long compradorId);

    List<Order> findAllByOrderByFechaDesc();

    long countByEstado(EstadoOrden estado);

    Optional<Order> findByIdAndCompradorId(Long id, Long compradorId);
}