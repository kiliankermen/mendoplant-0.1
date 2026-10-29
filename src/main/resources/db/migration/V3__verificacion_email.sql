-- =====================================================================
-- V3: Verificación de email en el registro (evitar cuentas fantasmas).
-- ---------------------------------------------------------------------
-- Guarda los códigos de verificación de 6 dígitos que se envían por email.
-- La cuenta de usuario SOLO se crea cuando el registro confirma un código
-- válido (no se necesita columna de estado en `usuarios`).
-- ---------------------------------------------------------------------
CREATE TABLE verificaciones_email (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    email       VARCHAR(255) NOT NULL,
    codigo      CHAR(6)      NOT NULL,
    expira_en   DATETIME     NOT NULL,
    usada       TINYINT(1)   NOT NULL DEFAULT 0,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  VARCHAR(100) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NULL,
    modified_at DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_verificacion_email (email),
    KEY idx_verificacion_email_codigo (email, codigo, usada)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;