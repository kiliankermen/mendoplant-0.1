package com.mendoplant.app.repository;

import com.mendoplant.app.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de ventas usadas por el dashboard del vendedor.
 * El parámetro {@code vendedorId = -1} significa "todos" (vista de administrador).
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = """
            SELECT p.id, p.nombre_comun, p.imagen_url,
                   COALESCE(SUM(i.precio_unitario * i.cantidad), 0) AS monto,
                   COALESCE(SUM(i.cantidad), 0) AS cantidad
              FROM orden_items i
              JOIN ordenes o ON o.id = i.orden_id
              JOIN plantas p ON p.id = i.planta_id
             WHERE o.estado = 'COMPLETADO'
               AND (:vendedorId = -1 OR p.vendedor_id = :vendedorId)
             GROUP BY p.id, p.nombre_comun, p.imagen_url
            """, nativeQuery = true)
    List<Object[]> ventasPorProducto(@Param("vendedorId") Long vendedorId);

    @Query(value = """
            SELECT DATE_FORMAT(o.fecha, '%Y-%m') AS periodo,
                   COALESCE(SUM(i.precio_unitario * i.cantidad), 0) AS monto
              FROM orden_items i
              JOIN ordenes o ON o.id = i.orden_id
              JOIN plantas p ON p.id = i.planta_id
             WHERE o.estado = 'COMPLETADO'
               AND (:vendedorId = -1 OR p.vendedor_id = :vendedorId)
             GROUP BY periodo
             ORDER BY periodo
            """, nativeQuery = true)
    List<Object[]> ventasPorMes(@Param("vendedorId") Long vendedorId);

    /**
     * Ventas agrupadas por día dentro de un rango de fechas. Devuelve
     * [0]=fecha (yyyy-MM-dd), [1]=monto, [2]=cantidad de unidades.
     */
    @Query(value = """
            SELECT DATE(o.fecha) AS dia,
                   COALESCE(SUM(i.precio_unitario * i.cantidad), 0) AS monto,
                   COALESCE(SUM(i.cantidad), 0) AS cantidad
              FROM orden_items i
              JOIN ordenes o ON o.id = i.orden_id
              JOIN plantas p ON p.id = i.planta_id
             WHERE o.estado = 'COMPLETADO'
               AND o.fecha >= :desde
               AND o.fecha < :hasta
               AND (:vendedorId = -1 OR p.vendedor_id = :vendedorId)
             GROUP BY dia
             ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> ventasPorDia(@Param("vendedorId") Long vendedorId,
                                @Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);

    /**
     * Ventas agrupadas por mes dentro de un rango de fechas. Devuelve
     * [0]=periodo (yyyy-MM), [1]=monto, [2]=cantidad de unidades.
     */
    @Query(value = """
            SELECT DATE_FORMAT(o.fecha, '%Y-%m') AS periodo,
                   COALESCE(SUM(i.precio_unitario * i.cantidad), 0) AS monto,
                   COALESCE(SUM(i.cantidad), 0) AS cantidad
              FROM orden_items i
              JOIN ordenes o ON o.id = i.orden_id
              JOIN plantas p ON p.id = i.planta_id
             WHERE o.estado = 'COMPLETADO'
               AND o.fecha >= :desde
               AND o.fecha < :hasta
               AND (:vendedorId = -1 OR p.vendedor_id = :vendedorId)
             GROUP BY periodo
             ORDER BY periodo
            """, nativeQuery = true)
    List<Object[]> ventasPorMesRango(@Param("vendedorId") Long vendedorId,
                                     @Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta);

    /**
     * Órdenes que incluyen al menos un producto del vendedor (para el registro
     * histórico). Devuelve [0]=orden_id, [1]=fecha, [2]=total, [3]=comprador_id,
     * [4]=cantidad de unidades del vendedor, [5]=monto del vendedor.
     * {@code vendedorId = -1} significa todas (vista admin).
     */
    @Query(value = """
            SELECT o.id, o.fecha, o.total, o.comprador_id,
                   COALESCE(SUM(i.cantidad), 0) AS cantidad,
                   COALESCE(SUM(i.precio_unitario * i.cantidad), 0) AS monto
              FROM orden_items i
              JOIN ordenes o ON o.id = i.orden_id
              JOIN plantas p ON p.id = i.planta_id
             WHERE o.estado = 'COMPLETADO'
               AND (:vendedorId = -1 OR p.vendedor_id = :vendedorId)
             GROUP BY o.id, o.fecha, o.total, o.comprador_id
             ORDER BY o.fecha DESC
            """, nativeQuery = true)
    List<Object[]> ordenesDelVendedor(@Param("vendedorId") Long vendedorId);
}
