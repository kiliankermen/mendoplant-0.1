package com.mendoplant.app.service.impl;

import com.mendoplant.app.model.SiteConfig;
import com.mendoplant.app.repository.SiteConfigRepository;
import com.mendoplant.app.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteConfigServiceImpl implements SiteConfigService {

    private final SiteConfigRepository siteConfigRepository;

    @Override
    public String obtener(String clave, String valorPorDefecto) {
        return siteConfigRepository.findByClave(clave)
                .map(SiteConfig::getValor)
                .orElse(valorPorDefecto);
    }

    @Override
    @Transactional
    public void guardar(String clave, String valor) {
        SiteConfig config = siteConfigRepository.findByClave(clave)
                .orElseGet(() -> SiteConfig.builder().clave(clave).build());
        config.setValor(valor);
        siteConfigRepository.save(config);
    }
}
