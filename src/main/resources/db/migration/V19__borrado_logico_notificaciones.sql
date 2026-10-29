-- Borrado lógico de notificaciones: permite vaciar la bandeja sin que los
-- avisos base/promociones se vuelvan a crear en la siguiente visita.
ALTER TABLE notificaciones
    ADD COLUMN eliminada BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE usuarios
    DROP COLUMN notificaciones_base_creadas;
