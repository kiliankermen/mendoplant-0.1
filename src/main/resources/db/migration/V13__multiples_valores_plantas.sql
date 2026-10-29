-- =====================================================================
-- V13: Multiples valores por atributo de planta.
-- ---------------------------------------------------------------------
--  Cada atributo (ambiente, clima, luz, cuidado, tipo de planta, tamaño,
--  maceta, sustrato y accesorio) pasa de ser UNA columna VARCHAR a ser una
--  tabla puente. Así una planta puede tener varias etiquetas de la misma
--  categoría (por ejemplo ambiente INTERIOR + EXTERIOR) y los filtros
--  pueden acumularse dentro de una misma categoría.
--
--  1) Se crean las tablas puente.
--  2) Se copia el valor único que había en cada columna.
--  3) Se eliminan las columnas viejas (ya no las usa la entidad).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tablas puente
-- ---------------------------------------------------------------------
CREATE TABLE plantas_ambiente (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(20) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_ambiente_valor (valor),
    CONSTRAINT fk_plantas_ambiente FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plantas_clima (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(20) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_clima_valor (valor),
    CONSTRAINT fk_plantas_clima FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plantas_luz (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(20) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_luz_valor (valor),
    CONSTRAINT fk_plantas_luz FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plantas_nivel_cuidado (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(20) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_nivel_cuidado_valor (valor),
    CONSTRAINT fk_plantas_nivel_cuidado FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plantas_tipo_planta (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(30) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_tipo_planta_valor (valor),
    CONSTRAINT fk_plantas_tipo_planta FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plantas_tamano (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(20) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_tamano_valor (valor),
    CONSTRAINT fk_plantas_tamano FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plantas_tipo_maceta (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(20) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_tipo_maceta_valor (valor),
    CONSTRAINT fk_plantas_tipo_maceta FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plantas_tipo_sustrato (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(30) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_tipo_sustrato_valor (valor),
    CONSTRAINT fk_plantas_tipo_sustrato FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plantas_tipo_accesorio (
    planta_id BIGINT      NOT NULL,
    valor     VARCHAR(20) NOT NULL,
    PRIMARY KEY (planta_id, valor),
    KEY idx_plantas_tipo_accesorio_valor (valor),
    CONSTRAINT fk_plantas_tipo_accesorio FOREIGN KEY (planta_id) REFERENCES plantas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Migración de datos: el valor único de cada columna pasa a la tabla puente
-- ---------------------------------------------------------------------
INSERT INTO plantas_ambiente       (planta_id, valor) SELECT id, ambiente       FROM plantas WHERE ambiente       IS NOT NULL AND ambiente       <> '';
INSERT INTO plantas_clima          (planta_id, valor) SELECT id, clima          FROM plantas WHERE clima          IS NOT NULL AND clima          <> '';
INSERT INTO plantas_luz            (planta_id, valor) SELECT id, luz            FROM plantas WHERE luz            IS NOT NULL AND luz            <> '';
INSERT INTO plantas_nivel_cuidado  (planta_id, valor) SELECT id, nivel_cuidado  FROM plantas WHERE nivel_cuidado  IS NOT NULL AND nivel_cuidado  <> '';
INSERT INTO plantas_tipo_planta    (planta_id, valor) SELECT id, tipo_planta    FROM plantas WHERE tipo_planta    IS NOT NULL AND tipo_planta    <> '';
INSERT INTO plantas_tamano         (planta_id, valor) SELECT id, tamano         FROM plantas WHERE tamano         IS NOT NULL AND tamano         <> '';
INSERT INTO plantas_tipo_maceta    (planta_id, valor) SELECT id, tipo_maceta    FROM plantas WHERE tipo_maceta    IS NOT NULL AND tipo_maceta    <> '';
INSERT INTO plantas_tipo_sustrato  (planta_id, valor) SELECT id, tipo_sustrato  FROM plantas WHERE tipo_sustrato  IS NOT NULL AND tipo_sustrato  <> '';
INSERT INTO plantas_tipo_accesorio (planta_id, valor) SELECT id, tipo_accesorio FROM plantas WHERE tipo_accesorio IS NOT NULL AND tipo_accesorio <> '';

-- ---------------------------------------------------------------------
-- Se eliminan las columnas de valor único
-- ---------------------------------------------------------------------
ALTER TABLE plantas
    DROP COLUMN tamano,
    DROP COLUMN luz,
    DROP COLUMN tipo_maceta,
    DROP COLUMN tipo_sustrato,
    DROP COLUMN tipo_accesorio,
    DROP COLUMN ambiente,
    DROP COLUMN clima,
    DROP COLUMN nivel_cuidado,
    DROP COLUMN tipo_planta;
