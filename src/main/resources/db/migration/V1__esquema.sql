-- =====================================================================
-- V1: Esquema inicial de Mendo Plant (MySQL 8 / InnoDB / utf8mb4)
-- ---------------------------------------------------------------------
-- Convenciones de integridad:
--  * Todas las tablas son InnoDB, charset utf8mb4.
--  * Todas las entidades "versionadas" llevan columna version BIGINT
--    para optimistic locking (JPA @Version).
--  * Columna de auditoría estándar en las tablas principales:
--      created_by, created_at, modified_by, modified_at.
--  * Eliminación lógica (soft delete):
--      - categorias/blog_posts/guias_cuidado: columna `activo`.
--      - plantas: columna `activo` (retiro de catálogo sin borrado físico).
--      - promociones/ofertas_dia: columna `activa`.
--      - usuarios: columna `estado`.
--  * Índices sobre todas las FK y sobre las consultas frecuentes.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    proveedor      VARCHAR(20)  NOT NULL,
    provider_id    VARCHAR(100)          DEFAULT '',
    fecha_registro DATETIME     NOT NULL,
    estado         TINYINT(1)   NOT NULL DEFAULT 1,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_by     VARCHAR(100) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by    VARCHAR(100) NULL,
    modified_at    DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuarios_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Categorías
-- ---------------------------------------------------------------------
CREATE TABLE categorias (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(60)  NOT NULL,
    descripcion VARCHAR(255) NULL,
    activo      TINYINT(1)   NOT NULL DEFAULT 1,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  VARCHAR(100) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NULL,
    modified_at DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categorias_nombre (nombre),
    KEY idx_categorias_activo (activo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Plantas  (tabla principal del catálogo)
-- ---------------------------------------------------------------------
CREATE TABLE plantas (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    nombre_comun       VARCHAR(120)  NOT NULL,
    nombre_cientifico  VARCHAR(150)  NULL,
    descripcion        VARCHAR(2000) NULL,
    precio             DECIMAL(10,2) NOT NULL,
    stock              INT           NOT NULL,
    imagen_url         VARCHAR(500)  NULL,
    fecha_publicacion  DATETIME      NOT NULL,
    categoria_id       BIGINT        NULL,
    tamano             VARCHAR(20)   NOT NULL,
    luz                VARCHAR(20)   NOT NULL,
    tipo_maceta        VARCHAR(20)   NULL,
    tipo_sustrato      VARCHAR(30)   NULL,
    tipo_accesorio     VARCHAR(20)   NULL,
    ambiente           VARCHAR(20)   NULL,
    clima              VARCHAR(20)   NULL,
    nivel_cuidado      VARCHAR(20)   NULL,
    tipo_planta        VARCHAR(30)   NULL,
    es_combo           TINYINT(1)    NOT NULL DEFAULT 0,
    vendedor_id        BIGINT        NOT NULL,
    activo             TINYINT(1)    NOT NULL DEFAULT 1,
    version            BIGINT        NOT NULL DEFAULT 0,
    created_by         VARCHAR(100)  NULL,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by        VARCHAR(100)  NULL,
    modified_at        DATETIME      NULL,
    PRIMARY KEY (id),
    KEY idx_plantas_categoria (categoria_id),
    KEY idx_plantas_vendedor (vendedor_id),
    KEY idx_plantas_activo (activo),
    KEY idx_plantas_activo_fecha (activo, fecha_publicacion),
    KEY idx_plantas_amb_clima (ambiente, clima),
    KEY idx_plantas_tipo_planta (tipo_planta),
    KEY idx_plantas_combo (activo, es_combo),
    CONSTRAINT fk_plantas_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_plantas_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Guías de cuidado
-- ---------------------------------------------------------------------
CREATE TABLE guias_cuidado (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    titulo_ficha      VARCHAR(150)  NOT NULL,
    planta_id         BIGINT        NULL,
    categoria_generica VARCHAR(60)  NULL,
    frecuencia_riego  VARCHAR(150)  NULL,
    humedad           VARCHAR(150)  NULL,
    temperatura       VARCHAR(150)  NULL,
    recomendaciones   LONGTEXT      NULL,
    activo            TINYINT(1)    NOT NULL DEFAULT 1,
    version           BIGINT        NOT NULL DEFAULT 0,
    created_by        VARCHAR(100)  NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by       VARCHAR(100)  NULL,
    modified_at       DATETIME      NULL,
    PRIMARY KEY (id),
    KEY idx_guias_planta (planta_id),
    CONSTRAINT fk_guias_planta FOREIGN KEY (planta_id) REFERENCES plantas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Blog
-- ---------------------------------------------------------------------
CREATE TABLE blog_posts (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    titulo      VARCHAR(200)  NOT NULL,
    slug        VARCHAR(220)  NOT NULL,
    contenido   LONGTEXT      NOT NULL,
    autor_id    BIGINT        NULL,
    fecha       DATETIME      NOT NULL,
    categoria   VARCHAR(60)   NULL,
    activo      TINYINT(1)    NOT NULL DEFAULT 1,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_by  VARCHAR(100)  NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100)  NULL,
    modified_at DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_posts_slug (slug),
    KEY idx_posts_autor (autor_id),
    KEY idx_posts_fecha (fecha),
    CONSTRAINT fk_posts_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Configuración clave/valor del sitio
-- ---------------------------------------------------------------------
CREATE TABLE site_config (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    clave       VARCHAR(80)  NOT NULL,
    valor       LONGTEXT     NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  VARCHAR(100) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NULL,
    modified_at DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_site_config_clave (clave)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Notificaciones
-- ---------------------------------------------------------------------
CREATE TABLE notificaciones (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id  BIGINT       NOT NULL,
    icono       VARCHAR(10)  NOT NULL,
    titulo      VARCHAR(120) NOT NULL,
    mensaje     VARCHAR(300) NOT NULL,
    fecha       DATETIME     NOT NULL,
    leida       TINYINT(1)   NOT NULL DEFAULT 0,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  VARCHAR(100) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NULL,
    modified_at DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_notif_usuario (usuario_id),
    KEY idx_notif_usuario_fecha (usuario_id, fecha),
    CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Ordenes de compra (transacciones; nunca se borran)
-- ---------------------------------------------------------------------
CREATE TABLE ordenes (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    comprador_id   BIGINT        NOT NULL,
    fecha          DATETIME      NOT NULL,
    total          DECIMAL(10,2) NOT NULL,
    estado         VARCHAR(20)   NOT NULL,
    preference_id  VARCHAR(40)   NULL,
    payment_id     BIGINT        NULL,
    payment_status VARCHAR(30)   NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_by     VARCHAR(100)  NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by    VARCHAR(100)  NULL,
    modified_at    DATETIME      NULL,
    PRIMARY KEY (id),
    KEY idx_ordenes_comprador (comprador_id),
    KEY idx_ordenes_estado (estado),
    KEY idx_ordenes_fecha (fecha),
    CONSTRAINT fk_ordenes_comprador FOREIGN KEY (comprador_id) REFERENCES usuarios (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Ítems de órdenes (detalle de línea; append-only)
-- ---------------------------------------------------------------------
CREATE TABLE orden_items (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    orden_id        BIGINT        NOT NULL,
    planta_id       BIGINT        NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_items_orden (orden_id),
    KEY idx_items_planta (planta_id),
    CONSTRAINT fk_items_orden FOREIGN KEY (orden_id) REFERENCES ordenes (id),
    CONSTRAINT fk_items_planta FOREIGN KEY (planta_id) REFERENCES plantas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Envíos (uno por orden)
-- ---------------------------------------------------------------------
CREATE TABLE envios (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    orden_id         BIGINT        NOT NULL,
    estado           VARCHAR(20)   NOT NULL,
    receptor_nombre  VARCHAR(120)  NOT NULL,
    telefono         VARCHAR(30)   NULL,
    calle_numero     VARCHAR(200)  NOT NULL,
    depto            VARCHAR(50)   NULL,
    ciudad           VARCHAR(80)   NOT NULL,
    codigo_postal    VARCHAR(12)   NULL,
    provincia        VARCHAR(60)   NOT NULL,
    instrucciones    VARCHAR(300)  NULL,
    costo            DECIMAL(10,2) NOT NULL,
    transportista    VARCHAR(60)   NULL,
    tracking         VARCHAR(100)  NULL,
    fecha_creacion   DATETIME      NOT NULL,
    fecha_envio      DATETIME      NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    created_by       VARCHAR(100)  NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by      VARCHAR(100)  NULL,
    modified_at      DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_envios_orden (orden_id),
    KEY idx_envios_estado (estado),
    CONSTRAINT fk_envios_orden FOREIGN KEY (orden_id) REFERENCES ordenes (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Facturas (comprobantes fiscales; nunca se borran, solo se anulan)
-- ---------------------------------------------------------------------
CREATE TABLE facturas (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    orden_id              BIGINT        NOT NULL,
    numero                VARCHAR(20)   NOT NULL,
    punto_venta           INT           NOT NULL,
    letra                 VARCHAR(1)    NOT NULL,
    fecha_emision         DATETIME      NOT NULL,
    cuit_emisor           VARCHAR(11)   NULL,
    razon_social          VARCHAR(120)  NOT NULL,
    domicilio_emisor      VARCHAR(200)  NULL,
    condicion_fiscal_emisor VARCHAR(80) NULL,
    nombre_receptor       VARCHAR(120)  NOT NULL,
    documento_receptor    VARCHAR(80)   NULL,
    subtotal              DECIMAL(12,2) NOT NULL,
    iva                   DECIMAL(12,2) NOT NULL,
    total                 DECIMAL(12,2) NOT NULL,
    estado                VARCHAR(20)   NOT NULL DEFAULT 'FACTURADA',
    version               BIGINT        NOT NULL DEFAULT 0,
    created_by            VARCHAR(100)  NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by           VARCHAR(100)  NULL,
    modified_at           DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_facturas_numero (numero),
    KEY idx_facturas_orden (orden_id),
    KEY idx_facturas_letra (letra),
    CONSTRAINT fk_facturas_orden FOREIGN KEY (orden_id) REFERENCES ordenes (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Wishlist (favoritos de usuario)
-- ---------------------------------------------------------------------
CREATE TABLE wishlist (
    usuario_id BIGINT NOT NULL,
    planta_id  BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, planta_id),
    KEY idx_wishlist_planta (planta_id),
    CONSTRAINT fk_wishlist_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_wishlist_planta FOREIGN KEY (planta_id) REFERENCES plantas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Promociones de descuento por porcentaje (configurables desde admin)
--   - porcentaje  : descuento aplicado (1..100).
--   - dias_semana : "1,3,5" (ISO: 1=lunes..7=domingo). Vacío = todos los días.
--   - fecha_inicio/fecha_fin : ventana de vigencia (opcional).
--   - tipo_aplicacion : TODAS | CATEGORIA | PLANTAS  (+ filtro opcional por clima).
--   - eliminación lógica: columna `activa`.
-- ---------------------------------------------------------------------
CREATE TABLE promociones (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(120) NOT NULL,
    porcentaje      DECIMAL(5,2) NOT NULL,
    dias_semana     VARCHAR(20)  NOT NULL DEFAULT '',
    fecha_inicio    DATE         NULL,
    fecha_fin       DATE         NULL,
    tipo_aplicacion VARCHAR(20)  NOT NULL,
    categoria_id    BIGINT       NULL,
    clima           VARCHAR(20)  NULL,
    activa          TINYINT(1)   NOT NULL DEFAULT 1,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      VARCHAR(100) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by     VARCHAR(100) NULL,
    modified_at     DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_promo_activa (activa),
    KEY idx_promo_categoria (categoria_id),
    KEY idx_promo_clima (clima),
    CONSTRAINT fk_promo_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT chk_promo_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Plantas incluidas explícitamente en una promoción (tipo_aplicacion = PLANTAS)
CREATE TABLE promocion_plantas (
    promo_id  BIGINT NOT NULL,
    planta_id BIGINT NOT NULL,
    PRIMARY KEY (promo_id, planta_id),
    KEY idx_promo_plantas_planta (planta_id),
    CONSTRAINT fk_pp_promo FOREIGN KEY (promo_id) REFERENCES promociones (id),
    CONSTRAINT fk_pp_planta FOREIGN KEY (planta_id) REFERENCES plantas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Ofertas del día: UNA planta destacada por día de la semana, con su %
-- de descuento. dia_semana usa ISO: 1=lunes, 2=martes ... 7=domingo.
-- El admin edita qué planta toca cada día y con qué descuento.
-- ---------------------------------------------------------------------
CREATE TABLE ofertas_dia (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    dia_semana  INT          NOT NULL,
    planta_id   BIGINT       NOT NULL,
    porcentaje  DECIMAL(5,2) NOT NULL,
    activa      TINYINT(1)   NOT NULL DEFAULT 1,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  VARCHAR(100) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NULL,
    modified_at DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ofertas_dia_semana (dia_semana),
    KEY idx_ofertas_planta (planta_id),
    CONSTRAINT fk_ofertas_planta FOREIGN KEY (planta_id) REFERENCES plantas (id),
    CONSTRAINT chk_oferta_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_oferta_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;