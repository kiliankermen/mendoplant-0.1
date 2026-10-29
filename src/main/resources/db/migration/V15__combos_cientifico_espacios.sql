-- ---------------------------------------------------------------------
-- Los combos generados desde el dashboard guardaban el nombre científico
-- con guiones (por ejemplo "Combo-Zamioculca-+-Potus"). Esos guiones se
-- mostraban en las tarjetas del catálogo. El nombre visible debe conservar
-- los espacios; los guiones se generan únicamente al construir la URL.
-- ---------------------------------------------------------------------

UPDATE plantas
   SET nombre_cientifico = REPLACE(nombre_cientifico, '-', ' ')
 WHERE es_combo = 1
   AND nombre_cientifico = REPLACE(nombre_comun, ' ', '-');
