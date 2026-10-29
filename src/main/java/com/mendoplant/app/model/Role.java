package com.mendoplant.app.model;

/**
 * Roles disponibles para los usuarios de la plataforma.
 * El rol VENDEDOR lo asigna el administrador desde el panel (/admin/usuarios):
 * solo vendedores (y admin) pueden publicar plantas.
 */
public enum Role {
    ROLE_USER,
    ROLE_VENDEDOR,
    ROLE_ADMIN
}
