package com.mendoplant.app.service.impl;

import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.Factura;
import com.mendoplant.app.model.Order;
import com.mendoplant.app.repository.FacturaRepository;
import com.mendoplant.app.repository.OrderRepository;
import com.mendoplant.app.service.FacturaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Emisión de comprobantes fiscales. Implementación <b>local</b>: genera un CFE
 * numerado y persistido (descargable/exportable), sin contacto con AFIP.
 *
 * <p>Para facturar ante AFIP (WSAA + WSFEv1) se necesita certificado fiscal de una
 * empresa registrada; el punto de integración queda indicado por
 * {@code app.facturacion.proveedor=afip} (a implementar). El resto de la app es
 * independiente de ese detalle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaServiceImpl implements FacturaService {

    private static final BigDecimal IVA = new BigDecimal("0.21");
    private static final BigDecimal BASE_POR_IVA = new BigDecimal("1.21");
    private static final String ESTADO_FACTURADA = "FACTURADA";

    private final FacturaRepository facturaRepository;
    private final OrderRepository orderRepository;

    @Value("${app.facturacion.punto-venta:1}")
    private int puntoVenta;

    @Value("${app.facturacion.letra:C}")
    private String letra;

    @Value("${app.facturacion.cuit:}")
    private String cuit;

    @Value("${app.facturacion.razon-social:Mendo Plant}")
    private String razonSocial;

    @Value("${app.facturacion.domicilio:Mendoza, Argentina}")
    private String domicilio;

    @Value("${app.facturacion.condicion-fiscal:Monotributista}")
    private String condicionFiscal;

    @Override
    @Transactional
    public Factura emitirPara(Long ordenId) {
        Order order = orderRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + ordenId));
        return emitirPara(order);
    }

    @Override
    @Transactional
    public Factura emitirPara(Order order) {
        Factura existente = buscarPorOrden(order.getId());
        if (existente != null && ESTADO_FACTURADA.equals(existente.getEstado())) {
            return existente;
        }
        // Si la anterior fue anulada, se emite una nueva numeración.
        return generar(order);
    }

    private Factura generar(Order order) {
        long correlativo = facturaRepository.countByLetra(letra.toUpperCase()) + 1;
        String numero = String.format("%05d-%08d", puntoVenta, correlativo);

        BigDecimal total = order.getTotal();
        BigDecimal subtotal;
        BigDecimal iva;
        if ("A".equalsIgnoreCase(letra)) {
            subtotal = total.divide(BASE_POR_IVA, 2, RoundingMode.HALF_UP);
            iva = total.subtract(subtotal);
        } else {
            subtotal = total;
            iva = BigDecimal.ZERO;
        }

        Factura factura = Factura.builder()
                .orden(order)
                .numero(numero)
                .puntoVenta(puntoVenta)
                .letra(letra.toUpperCase())
                .fechaEmision(LocalDateTime.now())
                .cuitEmisor(cuit)
                .razonSocial(razonSocial)
                .domicilioEmisor(domicilio)
                .condicionFiscalEmisor(condicionFiscal)
                .nombreReceptor(order.getComprador().getNombre())
                .documentoReceptor("Consumidor Final")
                .subtotal(subtotal)
                .iva(iva)
                .total(total)
                .estado(ESTADO_FACTURADA)
                .build();

        Factura guardada = facturaRepository.save(factura);
        log.info("Factura {} emitida para la orden {}", guardada.getNumero(), order.getId());
        return guardada;
    }

    @Override
    public Factura buscarPorId(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con id: " + id));
    }

    @Override
    public Factura buscarPorOrden(Long ordenId) {
        return facturaRepository.findFirstByOrdenIdOrderByIdDesc(ordenId).orElse(null);
    }

    @Override
    public List<Factura> listarTodas() {
        return facturaRepository.findAll();
    }

    @Override
    @Transactional
    public void anular(Long facturaId) {
        Factura factura = buscarPorId(facturaId);
        if ("ANULADA".equals(factura.getEstado())) {
            return;
        }
        factura.setEstado("ANULADA");
        facturaRepository.save(factura);
        log.info("Factura {} anulada", factura.getNumero());
    }

    // Método auxiliar expuesto para validar la letra configurada sin romper si es inválida.
    @SuppressWarnings("unused")
    private void validarConfiguracion() {
        if (!"A".equalsIgnoreCase(letra) && !"C".equalsIgnoreCase(letra)) {
            throw new BusinessException("Letra de facturación inválida: " + letra);
        }
    }
}