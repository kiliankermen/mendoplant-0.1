-- Color personalizado del cartel de la promoción en la portada.
ALTER TABLE promociones
    ADD COLUMN color_banner VARCHAR(7) NULL AFTER porcentaje;
