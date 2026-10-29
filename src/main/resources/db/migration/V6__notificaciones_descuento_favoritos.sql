-- V6: notificaciones de descuento con planta asociada
-- Las notificaciones de descuento en favoritos se vinculan a la planta para
-- poder enlazar la bandeja con la página de detalle de la planta.

ALTER TABLE notificaciones
    ADD COLUMN planta_id BIGINT NULL AFTER usuario_id;

ALTER TABLE notificaciones
    ADD KEY idx_notif_planta (planta_id),
    ADD CONSTRAINT fk_notif_planta FOREIGN KEY (planta_id) REFERENCES plantas (id);