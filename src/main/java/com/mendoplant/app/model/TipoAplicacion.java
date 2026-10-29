package com.mendoplant.app.model;

/**
 * Alcance de aplicación de una Promocion:
 * TODAS = todas las plantas (puede combinarse con el filtro por clima),
 * CATEGORIA = plantas de una categoría puntual,
 * PLANTAS = solo las plantas listadas en promocion_plantas.
 */
public enum TipoAplicacion {
    TODAS, CATEGORIA, PLANTAS
}