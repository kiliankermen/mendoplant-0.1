-- ---------------------------------------------------------------------
-- Umbral de envío gratis (fuente única para front y back).
--   - Front: tarjeta "Envíos gratis" (Compras mayores a $X sin coste de envío).
--   - Back: cálculo del costo de envío del checkout (EnvioServiceImpl).
-- Cambio: de $30.000 a $90.000.
-- ---------------------------------------------------------------------
INSERT INTO site_config (clave, valor, created_by, created_at)
VALUES ('ENVIO_GRATIS_DESDE', '90000', 'seed', NOW())
ON DUPLICATE KEY UPDATE valor = '90000';