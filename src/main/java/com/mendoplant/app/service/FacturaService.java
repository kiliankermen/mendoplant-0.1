package com.mendoplant.app.service;

import com.mendoplant.app.model.Factura;
import com.mendoplant.app.model.Order;

import java.util.List;

public interface FacturaService {

    /** Emite la factura de una orden pagada. Idempotente: si ya existe, la devuelve. */
    Factura emitirPara(Order order);

    /** Relee la orden (nueva transacción) y emite/fuerza su facturación. Útil desde el panel admin. */
    Factura emitirPara(Long ordenId);

    Factura buscarPorId(Long id);

    Factura buscarPorOrden(Long ordenId);

    List<Factura> listarTodas();

    /** Anula lógicamente una factura (se conserva el registro y el número). */
    void anular(Long facturaId);
}