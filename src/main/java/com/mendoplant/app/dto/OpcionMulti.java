package com.mendoplant.app.dto;

/**
 * Una opción de un desplegable con casillas de verificación: el valor crudo
 * del enum (para el formulario) y la etiqueta legible (para mostrar).
 */
public record OpcionMulti(String valor, String etiqueta, boolean seleccionado) {
}
