-- Carrito persistente: guarda los ítems del carrito por usuario para que
-- no se pierdan al cerrar sesión (se recargan al volver a iniciar sesión).
CREATE TABLE carrito_item (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    usuario_id     BIGINT   NOT NULL,
    planta_id      BIGINT   NOT NULL,
    cantidad       INT      NOT NULL DEFAULT 1,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_carrito_usuario_planta (usuario_id, planta_id),
    KEY idx_carrito_planta (planta_id),
    CONSTRAINT fk_carrito_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_carrito_planta  FOREIGN KEY (planta_id)  REFERENCES plantas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;