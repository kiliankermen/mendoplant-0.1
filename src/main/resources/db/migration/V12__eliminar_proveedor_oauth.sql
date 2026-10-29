-- =====================================================================
-- V12: Eliminacion del proveedor de autenticacion (OAuth/Google).
-- ---------------------------------------------------------------------
--  La app quedo unicamente con login local (email + contrasena).
--  Se eliminan de la tabla usuarios las columnas:
--    * proveedor    : como se creo la cuenta (LOCAL / GOOGLE)
--    * provider_id  : identificador del proveedor externo (Google sub)
--
--  La columna "proveedor" era NOT NULL sin valor por defecto, por eso
--  hay que dropearla si no la entidad User la sigue mapeando (ya no lo
--  hace) y los INSERT de la app fallarian en MySQL.
-- =====================================================================

ALTER TABLE usuarios DROP COLUMN proveedor;
ALTER TABLE usuarios DROP COLUMN provider_id;
