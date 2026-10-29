package com.mendoplant.app.service.impl;

import com.mendoplant.app.dto.EnvioDTO;
import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.Envio;
import com.mendoplant.app.model.EstadoEnvio;
import com.mendoplant.app.model.Order;
import com.mendoplant.app.repository.EnvioRepository;
import com.mendoplant.app.repository.OrderRepository;
import com.mendoplant.app.service.EnvioService;
import com.mendoplant.app.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvioServiceImpl implements EnvioService {

    /** Clave en site_config con el umbral de envío gratis. */
    private static final String CLAVE_GRATIS_DESDE = "ENVIO_GRATIS_DESDE";

    private final EnvioRepository envioRepository;
    private final OrderRepository orderRepository;
    private final SiteConfigService siteConfigService;

    /** Tarifa plana de envío ($). 0 = gratis. */
    @Value("${app.envio.tarifa-plana:0}")
    private BigDecimal tarifaPlana;

    /** Umbral de envío gratis de respaldo (application.yml/entorno) si no hay fila en site_config. */
    @Value("${app.envio.gratis-desde:0}")
    private BigDecimal gratisDesdeRespaldo;

    /**
     * Umbral de envío gratis vigente. Fuente única: site_config (DB). Si la fila
     * no existe o el valor no es numérico, usa el de configuración.
     */
    private BigDecimal gratisDesde() {
        String valor = siteConfigService.obtener(CLAVE_GRATIS_DESDE,
                gratisDesdeRespaldo == null ? "0" : gratisDesdeRespaldo.toPlainString());
        try {
            return new BigDecimal(valor == null ? "0" : valor.trim());
        } catch (NumberFormatException e) {
            log.warn("Valor no numérico para {} en site_config: '{}'. Se usa el de configuración.", CLAVE_GRATIS_DESDE, valor);
            return gratisDesdeRespaldo == null ? BigDecimal.ZERO : gratisDesdeRespaldo;
        }
    }

    @Override
    public BigDecimal calcularCosto(String provincia, BigDecimal subtotalItems) {
        BigDecimal gratisDesde = gratisDesde();
        if (gratisDesde.compareTo(BigDecimal.ZERO) > 0
                && subtotalItems != null && subtotalItems.compareTo(gratisDesde) >= 0) {
            return BigDecimal.ZERO;
        }
        return tarifaPlana == null ? BigDecimal.ZERO : tarifaPlana;
    }

    @Override
    @Transactional
    public Envio crearEnvio(Order order, EnvioDTO dto) {
        BigDecimal costo = calcularCosto(dto.getProvincia(), order.getTotal());

        Envio envio = Envio.builder()
                .orden(order)
                .receptorNombre(dto.getReceptorNombre().trim())
                .telefono(dto.getTelefono())
                .calleNumero(dto.getCalleNumero().trim())
                .depto(dto.getDepto())
                .ciudad(dto.getCiudad().trim())
                .codigoPostal(dto.getCodigoPostal())
                .provincia(dto.getProvincia().trim())
                .instrucciones(dto.getInstrucciones())
                .costo(costo)
                .estado(EstadoEnvio.PENDIENTE)
                .build();

        order.setEnvio(envio);
        order.setTotal(order.getTotal().add(costo));
        orderRepository.save(order);

        log.info("Envío creado para orden {} con costo {}", order.getId(), costo);
        return envio;
    }

    @Override
    public Envio buscarPorOrden(Long ordenId) {
        return envioRepository.findByOrdenId(ordenId).orElse(null);
    }

    @Override
    @Transactional
    public void actualizarEstado(Long envioId, EstadoEnvio estado, String transportista, String tracking) {
        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado con id: " + envioId));
        if (estado != null) {
            envio.setEstado(estado);
            if (estado == EstadoEnvio.ENVIADO && envio.getFechaEnvio() == null) {
                envio.setFechaEnvio(LocalDateTime.now());
            }
        }
        if (transportista != null && !transportista.isBlank()) {
            envio.setTransportista(transportista.trim());
        }
        if (tracking != null && !tracking.isBlank()) {
            envio.setTracking(tracking.trim());
        }
        if (envio.getEstado() == EstadoEnvio.CANCELADO && envio.getOrden() != null
                && envio.getOrden().getEstado() != com.mendoplant.app.model.EstadoOrden.CANCELADO) {
            throw new BusinessException("No podés cancelar el envío de una orden no cancelada.");
        }
        envioRepository.save(envio);
    }
}