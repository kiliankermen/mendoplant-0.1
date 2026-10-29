-- Descripción personalizada del cartel de la promoción en la portada.
ALTER TABLE promociones
    ADD COLUMN descripcion VARCHAR(100) NULL AFTER nombre;
