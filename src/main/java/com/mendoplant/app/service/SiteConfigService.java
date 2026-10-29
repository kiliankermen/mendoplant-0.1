package com.mendoplant.app.service;

public interface SiteConfigService {
    String obtener(String clave, String valorPorDefecto);
    void guardar(String clave, String valor);
}
