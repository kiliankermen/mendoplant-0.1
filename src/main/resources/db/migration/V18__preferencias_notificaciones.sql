-- Preferencias de la bandeja de notificaciones.
-- notificaciones_silenciadas: oculta el rojo de las notificaciones nuevas.
-- notificaciones_base_creadas: evita que los avisos base reaparezcan tras eliminarlos.
ALTER TABLE usuarios
    ADD COLUMN notificaciones_silenciadas BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN notificaciones_base_creadas BOOLEAN NOT NULL DEFAULT FALSE;
