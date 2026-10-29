-- =====================================================================
-- V14: Dashboard del vendedor
-- ---------------------------------------------------------------------
--  1) Imágenes múltiples por producto (mínimo 2 al publicar desde el
--     dashboard). La primera imagen sigue viviendo en plantas.imagen_url
--     para no romper las tarjetas; la galería completa queda acá.
--  2) Todas las publicaciones existentes pasan a pertenecer a
--     kiliankermen005@gmail.com (dato inmutable desde la UI).
--  3) Las URLs de los productos usan guiones: se normalizan los nombres
--     científicos que pudieran tener guiones bajos.
-- =====================================================================

CREATE TABLE plantas_imagenes (
    planta_id BIGINT       NOT NULL,
    orden     INT          NOT NULL,
    url       VARCHAR(500) NOT NULL,
    PRIMARY KEY (planta_id, orden),
    CONSTRAINT fk_plantas_imagenes FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

UPDATE plantas p
  JOIN usuarios u ON u.email = 'kiliankermen005@gmail.com'
   SET p.vendedor_id = u.id;

UPDATE plantas
   SET nombre_cientifico = REPLACE(nombre_cientifico, '_', '-')
 WHERE nombre_cientifico LIKE '%\_%';
