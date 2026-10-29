package com.mendoplant.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cartel de promoción mostrado en la portada. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionDTO {
    private String titulo;
    private String subtitulo;
    private String badge;
    private String color;
    private String colorHex;
    private String slug;
    private String imagenUrl;
    private boolean activa;
    private String botonTexto;
    private String botonEnlace;
}