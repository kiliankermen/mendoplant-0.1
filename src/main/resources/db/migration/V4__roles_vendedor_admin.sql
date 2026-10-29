-- =====================================================================
-- V4: Rol VENDEDOR + administrador principal.
-- ---------------------------------------------------------------------
--  1) El usuario "kiliankermen005@gmail.com" es el administrador de la
--     plataforma (elije quién es vendedor desde el panel de administración).
--  2) El usuario demo (dueño de las publicaciones de ejemplo) pasa a ser
--     VENDEDOR para que siga pudiendo publicar/retirar sus plantas.
--
-- Contraseñas (BCrypt, verificadas con BCryptPasswordEncoder.matches):
--   kiliankermen005@gmail.com -> kilian#@!123
-- =====================================================================

UPDATE usuarios
   SET role = 'ROLE_VENDEDOR'
 WHERE email = 'usuario@mendoplant.com'
   AND role <> 'ROLE_ADMIN';

INSERT INTO usuarios (nombre, email, password, role, proveedor, provider_id, fecha_registro, estado, created_by, created_at) VALUES
 ('Kilian Kermen', 'kiliankermen005@gmail.com',
  '$2a$10$/v/Ly81HaZRx2lnfxB7u/.fOQthBOGRgUAkDT0z8zvw7P0UXvWiZG', 'ROLE_ADMIN', 'LOCAL', '', NOW(), 1, 'seed', NOW());