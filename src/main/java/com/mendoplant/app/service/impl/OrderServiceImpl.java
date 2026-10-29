package com.mendoplant.app.service.impl;

import com.mendoplant.app.dto.CheckoutItemDTO;
import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.*;
import com.mendoplant.app.repository.OrderRepository;
import com.mendoplant.app.repository.PlantRepository;
import com.mendoplant.app.repository.UserRepository;
import com.mendoplant.app.service.FacturaService;
import com.mendoplant.app.service.NotificacionService;
import com.mendoplant.app.service.OrderService;
import com.mendoplant.app.service.PlantService;
import com.mendoplant.app.service.PromocionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PlantRepository plantRepository;
    private final UserRepository userRepository;
    private final PlantService plantService;
    private final NotificacionService notificacionService;
    private final PromocionService promocionService;
    private final FacturaService facturaService;

    @Override
    @Transactional
    public Order crearOrden(Long compradorId, List<CheckoutItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("El carrito está vacío");
        }

        User comprador = userRepository.findById(compradorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + compradorId));

        Order order = Order.builder()
                .comprador(comprador)
                .estado(EstadoOrden.PENDIENTE)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CheckoutItemDTO item : items) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new BusinessException("La cantidad debe ser mayor a cero");
            }
            // Reserva de stock ATÓMICA: se descuenta primero (UPDATE condicional con
            // stock >= cantidad). Si no hay stock suficiente lanza BusinessException,
            // y como la transacción hace rollback no quedan descuentos parciales.
            plantService.descontarStock(item.getPlantId(), item.getCantidad());

            // Se recarga la planta ("fresca") para el item, tras el bulk update.
            Plant plant = plantRepository.findById(item.getPlantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Planta no encontrada con id: " + item.getPlantId()));

            OrderItem orderItem = OrderItem.builder()
                    .plant(plant)
                    .cantidad(item.getCantidad())
                    .precioUnitario(precioVigente(plant))
                    .build();
            order.addItem(orderItem);

            total = total.add(precioVigente(plant).multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        order.setTotal(total);
        return orderRepository.save(order);
    }

    @Override
    public List<Order> historialDe(Long compradorId) {
        return orderRepository.findByCompradorIdOrderByFechaDesc(compradorId);
    }

    @Override
    public Order buscarPorId(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));
    }

    @Override
    public Order buscarDelUsuario(Long id, Long usuarioId) {
        Order orden = buscarPorId(id);
        User actor = userRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        boolean esAdmin = actor.getRole().name().equals("ROLE_ADMIN");
        boolean esDuenio = orden.getComprador() != null && orden.getComprador().getId().equals(usuarioId);
        if (!esAdmin && !esDuenio) {
            throw new BusinessException("No tenés permiso para ver esta orden");
        }
        return orden;
    }

    @Override
    @Transactional
    public void registrarPagoAprobado(Long orderId, Long paymentId, String mpStatus) {
        Order order = buscarPorId(orderId);
        if (order.getEstado() == EstadoOrden.COMPLETADO) {
            return;
        }
        if (order.getEstado() == EstadoOrden.CANCELADO) {
            // Orden ya cancelada con stock repuesto: no la reactivamos ni duplicamos inventario.
            return;
        }
        order.setEstado(EstadoOrden.COMPLETADO);
        order.setPaymentId(paymentId);
        order.setPaymentStatus(mpStatus);
        orderRepository.save(order);

        // Cuenta las ventas: suma las unidades de cada línea a la planta vendida.
        for (OrderItem item : order.getItems()) {
            plantService.sumarVentas(item.getPlant().getId(), item.getCantidad());
        }

        facturaService.emitirPara(order);

        notificacionService.crear(order.getComprador().getId(), "✅", "Pago aprobado",
                "Tu pedido #" + order.getId() + " por $" + order.getTotal() + " fue acreditado.");
    }

    @Override
    @Transactional
    public void registrarPagoPendiente(Long orderId, Long paymentId, String mpStatus) {
        Order order = buscarPorId(orderId);
        if (order.getEstado() != EstadoOrden.PENDIENTE) {
            return;
        }
        order.setPaymentId(paymentId);
        order.setPaymentStatus(mpStatus);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void registrarPreferencia(Long orderId, String preferenceId) {
        Order order = buscarPorId(orderId);
        order.setPreferenceId(preferenceId);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void registrarPagoFallido(Long orderId) {
        Order order = buscarPorId(orderId);
        if (order.getEstado() != EstadoOrden.PENDIENTE) {
            return;
        }
        order.setEstado(EstadoOrden.CANCELADO);
        orderRepository.save(order);

        // Se repone el stock que había quedado reservado.
        for (OrderItem item : order.getItems()) {
            plantService.reponerStock(item.getPlant().getId(), item.getCantidad());
        }

        notificacionService.crear(order.getComprador().getId(), "❌", "Pago rechazado",
                "Tu pedido #" + order.getId() + " no se pudo cobrar y fue cancelado.");
    }

    @Override
    @Transactional
    public void completarSimulado(Long orderId) {
        registrarPagoAprobado(orderId, null, "SIMULADO");
    }

    @Override
    public List<Order> listarTodas() {
        return orderRepository.findAllByOrderByFechaDesc();
    }

    @Override
    public long contarPorEstado(EstadoOrden estado) {
        return orderRepository.countByEstado(estado);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long orderId, EstadoOrden nuevoEstado) {
        Order order = buscarPorId(orderId);
        EstadoOrden actual = order.getEstado();
        if (nuevoEstado == null || nuevoEstado == actual) {
            return;
        }
        if (actual != EstadoOrden.PENDIENTE) {
            throw new BusinessException("Solo se pueden cancelar órdenes pendientes de pago.");
        }
        if (nuevoEstado == EstadoOrden.CANCELADO) {
            registrarPagoFallido(orderId);
            return;
        }
        throw new BusinessException("Transición de estado no permitida: " + actual + " -> " + nuevoEstado);
    }

    /** Precio final que cobra el comprador (aplica el mejor descuento vigente, si existe). */
    private BigDecimal precioVigente(Plant plant) {
        return promocionService.precioConDescuento(plant);
    }
}