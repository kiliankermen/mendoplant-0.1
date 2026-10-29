-- Contador de ventas por planta: alimenta el orden por defecto del catálogo
-- ("más vendidas primero") y los botones de ordenar por más/menos ventas.
-- Se incrementa con cada pago aprobado (ver OrderServiceImpl).
ALTER TABLE plantas ADD COLUMN ventas INT NOT NULL DEFAULT 0 AFTER activo;

-- Seed: simula un historial de ventas variado sobre el catálogo existente
-- para que el ordenamiento por ventas tenga datos desde el primer arranque.
UPDATE plantas SET ventas = (id * 7) % 43 + 2;