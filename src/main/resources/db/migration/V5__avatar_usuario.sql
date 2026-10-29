-- Foto de perfil subida por el usuario (URL publica, ej. /uploads/xxxx.png).
-- Si es NULL, el sistema genera un avatar con la inicial del nombre.
ALTER TABLE usuarios ADD COLUMN avatar_url VARCHAR(255) NULL;