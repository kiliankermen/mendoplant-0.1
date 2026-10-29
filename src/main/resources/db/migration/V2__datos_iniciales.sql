-- =====================================================================
-- V2: Datos iniciales de Mendo Plant
-- Contraseñas en texto plano de referencia (hasheadas con BCrypt abajo):
--   admin@mendoplant.com   -> Admin123!
--   usuario@mendoplant.com -> Usuario123!
-- =====================================================================

-- Usuarios por defecto (passwords hasheadas con BCrypt, válidas y verificadas)
INSERT INTO usuarios (id, nombre, email, password, role, proveedor, provider_id, fecha_registro, estado, created_by, created_at) VALUES
 (1, 'Administrador Mendo Plant', 'admin@mendoplant.com',
  '$2b$10$DV4sEnfq1dn1qAbvFBAvKemxqCWwL17cZzDAj2UuHVqZgmHlJYp36', 'ROLE_ADMIN', 'LOCAL', '', NOW(), 1, 'seed', NOW()),
 (2, 'Usuario Demo', 'usuario@mendoplant.com',
  '$2b$10$eO7iFNVvthyuSQJQyRtaZOKksXPb3hdO3JyKtUSy8jzLA2F1xzU4G', 'ROLE_USER', 'LOCAL', '', NOW(), 1, 'seed', NOW());

-- Categorías (las tres últimas son secciones del catálogo: productos y accesorios)
INSERT INTO categorias (id, nombre, descripcion, activo, created_by, created_at) VALUES
 (1, 'Interior', 'Plantas ideales para espacios interiores con luz indirecta', 1, 'seed', NOW()),
 (2, 'Exterior', 'Plantas resistentes para jardines, balcones y patios', 1, 'seed', NOW()),
 (3, 'Suculentas', 'Plantas de bajo mantenimiento que almacenan agua en sus hojas', 1, 'seed', NOW()),
 (4, 'Orquídeas', 'Plantas ornamentales de flor delicada', 1, 'seed', NOW()),
 (5, 'Plantas', 'Todas las plantas disponibles en la tienda', 1, 'seed', NOW()),
 (6, 'Macetas', 'Macetas y maceteros de distintos materiales', 1, 'seed', NOW()),
 (7, 'Soportes', 'Tutores, estacas y soportes para tus plantas', 1, 'seed', NOW()),
 (8, 'Sustratos', 'Sustratos y drenajes para el cuidado del suelo', 1, 'seed', NOW());

-- Plantas, productos y combos de ejemplo (sin es_oferta_dia: la oferta del día
-- se administra en la tabla ofertas_dia)
INSERT INTO plantas (
    id, nombre_comun, nombre_cientifico, descripcion, precio, stock, imagen_url,
    fecha_publicacion, categoria_id, tamano, luz, tipo_maceta, tipo_sustrato, tipo_accesorio, vendedor_id,
    activo, ambiente, clima, nivel_cuidado, tipo_planta, es_combo, created_by, created_at) VALUES
-- Interior / tropicales
 (1,  'Potus', 'Epipremnum aureum', 'Planta trepadora muy fácil de cuidar, ideal para principiantes.',
  4500.00, 25, '/images/placeholder-planta.svg', NOW(),
  1, 'PEQUENO', 'MEDIA_SOMBRA', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'TROPICAL', 'PRINCIPIANTE', 'ENREDADERA', 0, 'seed', NOW()),
 (2,  'Ficus Lyrata', 'Ficus lyrata', 'Planta de interior de grandes hojas en forma de violín.',
  12000.00, 10, '/images/placeholder-planta.svg', NOW(),
  1, 'GRANDE', 'MEDIA_SOMBRA', 'CERAMICA', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'TEMPLADO', 'INTERMEDIO', 'ARBOL', 0, 'seed', NOW()),
 (3,  'Monstera', 'Monstera deliciosa', 'La clásica costilla de Adán, de hojas gigantes y agujereadas.',
  15500.00, 12, '/images/placeholder-planta.svg', NOW(),
  5, 'GRANDE', 'MEDIA_SOMBRA', 'CERAMICA', 'DRENAJE_ALTO', NULL, 2, 1, 'INTERIOR', 'TROPICAL', 'PRINCIPIANTE', 'TROPICAL', 0, 'seed', NOW()),
 (4,  'Sansevieria', 'Dracaena trifasciata', 'Lengua de suegra. Casi indestructible, purifica el aire.',
  8200.00, 18, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'MEDIA_SOMBRA', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
 (5,  'Pothos Neón', 'Epipremnum aureum Neon', 'Variedad de hoja verde brillante que cae en cascada.',
  3900.00, 30, '/images/placeholder-planta.svg', NOW(),
  5, 'PEQUENO', 'SOMBRA', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'HUMEDO', 'PRINCIPIANTE', 'ENREDADERA', 0, 'seed', NOW()),
 (6,  'Palmera Areca', 'Dypsis lutescens', 'Palmera plumosa de interior que aporta frescura tropical.',
  16500.00, 7, '/images/placeholder-planta.svg', NOW(),
  1, 'GRANDE', 'MEDIA_SOMBRA', 'CERAMICA', 'DRENAJE_ALTO', NULL, 2, 1, 'INTERIOR', 'TROPICAL', 'INTERMEDIO', 'PALMERA', 0, 'seed', NOW()),
 (7,  'Filodendro Brasil', 'Philodendron hederaceum', 'Trepadora con hojas ribeteadas de verde y amarillo.',
  5600.00, 15, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'MEDIA_SOMBRA', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'TROPICAL', 'PRINCIPIANTE', 'ENREDADERA', 0, 'seed', NOW()),
 (8,  'Cinta', 'Chlorophytum comosum', 'Colgante resistente que produce plantitas hijas.',
  2400.00, 28, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'MEDIA_SOMBRA', 'BARRO', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'FLOR', 0, 'seed', NOW()),
 (9,  'Helecho de Boston', 'Nephrolepis exaltata', 'Frondas curvas y abundantes, perfecto para baños con humedad.',
  6800.00, 9, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'SOMBRA', 'BARRO', 'TURBA_PERLITA', NULL, 2, 1, 'INTERIOR', 'HUMEDO', 'INTERMEDIO', 'HELECHO', 0, 'seed', NOW()),
 (10, 'Alocasia Amazonica', 'Alocasia x amazonica', 'Hojas en punta de flecha con nervaduras marcadas.',
  9200.00, 11, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'MEDIA_SOMBRA', 'CERAMICA', 'TURBA_PERLITA', NULL, 2, 1, 'INTERIOR', 'HUMEDO', 'EXPERTO', 'TROPICAL', 0, 'seed', NOW()),
 (11, 'Maranta tricolor', 'Maranta leuconeura', 'Hojas que se abren de día y se cierran de noche.',
  7800.00, 6, '/images/placeholder-planta.svg', NOW(),
  5, 'PEQUENO', 'SOMBRA', 'CERAMICA', 'TURBA_PERLITA', NULL, 2, 1, 'INTERIOR', 'HUMEDO', 'INTERMEDIO', 'TROPICAL', 0, 'seed', NOW()),
-- Exterior
 (13, 'Lavanda', 'Lavandula angustifolia', 'Arbusto aromático de exterior, atrae polinizadores.',
  3200.00, 20, '/images/placeholder-planta.svg', NOW(),
  2, 'MEDIANO', 'SOL_DIRECTO', 'BARRO', 'DRENAJE_ALTO', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'AROMATICA', 0, 'seed', NOW()),
 (14, 'Azalea', 'Rhododendron simsii', 'Arbusto florido de espléndida floración primaveral.',
  7400.00, 14, '/images/placeholder-planta.svg', NOW(),
  2, 'MEDIANO', 'MEDIA_SOMBRA', 'PLASTICO', 'TURBA_PERLITA', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'INTERMEDIO', 'ARBUSTO', 0, 'seed', NOW()),
 (15, 'Rosal trepador', 'Rosa spp.', 'Rosal de flor perfumada ideal para paredes y pérgolas.',
  9800.00, 8, '/images/placeholder-planta.svg', NOW(),
  2, 'GRANDE', 'SOL_DIRECTO', 'BARRO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'INTERMEDIO', 'FLOR', 0, 'seed', NOW()),
 (16, 'Palmera Phoenix', 'Phoenix canariensis', 'Lenta pero imponente, clásica de jardines mediterráneos.',
  24000.00, 4, '/images/placeholder-planta.svg', NOW(),
  2, 'GIGANTE', 'SOL_DIRECTO', 'BARRO', 'DRENAJE_ALTO', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'EXPERTO', 'PALMERA', 0, 'seed', NOW()),
 (17, 'Jacarandá', 'Jacaranda mimosifolia', 'Árbol de flor violeta que pinta la ciudad en primavera.',
  18500.00, 5, '/images/placeholder-planta.svg', NOW(),
  2, 'GIGANTE', 'SOL_DIRECTO', 'BARRO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'INTERMEDIO', 'ARBOL', 0, 'seed', NOW()),
 (18, 'Buganvilla', 'Bougainvillea glabra', 'Enredadera de bracteas fucsias, lluviosa y luminosa.',
  8900.00, 13, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'SOL_DIRECTO', 'BARRO', 'DRENAJE_ALTO', NULL, 2, 1, 'EXTERIOR', 'TROPICAL', 'INTERMEDIO', 'ENREDADERA', 0, 'seed', NOW()),
 (19, 'Geranio', 'Pelargonium zonale', 'El clásico de balcones mendocinos, floración casi todo el año.',
  2600.00, 22, '/images/placeholder-planta.svg', NOW(),
  2, 'PEQUENO', 'SOL_DIRECTO', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'FLOR', 0, 'seed', NOW()),
 (20, 'Mandarino', 'Citrus reticulata', 'Cítrico enano que da frutos, ideal para maceta grande.',
  11800.00, 6, '/images/placeholder-planta.svg', NOW(),
  2, 'MEDIANO', 'SOL_DIRECTO', 'BARRO', 'DRENAJE_ALTO', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'INTERMEDIO', 'ARBOL', 0, 'seed', NOW()),
 (21, 'Platanera', 'Musa acuminata', 'Follaje tropical gigante para dar sombra y atmósfera jungla.',
  13500.00, 7, '/images/placeholder-planta.svg', NOW(),
  5, 'GIGANTE', 'SOL_DIRECTO', 'BARRO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TROPICAL', 'INTERMEDIO', 'TROPICAL', 0, 'seed', NOW()),
 (22, 'Heliconia', 'Heliconia rostrata', 'Inflorescencias colgantes de colores vivos, picas flores.',
  16900.00, 5, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'MEDIA_SOMBRA', 'BARRO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TROPICAL', 'EXPERTO', 'TROPICAL', 0, 'seed', NOW()),
-- Cactus
 (23, 'Cactus San Pedro', 'Echinopsis pachanoi', 'Cactus columnar de crecimiento rápido para exterior.',
  5600.00, 15, '/images/placeholder-planta.svg', NOW(),
  3, 'GRANDE', 'SOL_DIRECTO', 'BARRO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'EXTERIOR', 'SECO', 'PRINCIPIANTE', 'CACTUS', 0, 'seed', NOW()),
 (24, 'Biznaga', 'Mammillaria herrerae', 'Globosa con espinas blancas y corona de flores rosadas.',
  3400.00, 19, '/images/placeholder-planta.svg', NOW(),
  3, 'PEQUENO', 'SOL_DIRECTO', 'BARRO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'EXTERIOR', 'SECO', 'PRINCIPIANTE', 'CACTUS', 0, 'seed', NOW()),
 (25, 'Ferocactus', 'Ferocactus glaucescens', 'Barril de espinas doradas, lento pero muy longevo.',
  4700.00, 12, '/images/placeholder-planta.svg', NOW(),
  3, 'MEDIANO', 'SOL_DIRECTO', 'BARRO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'EXTERIOR', 'SECO', 'INTERMEDIO', 'CACTUS', 0, 'seed', NOW()),
 (26, 'Opuntia', 'Opuntia ficus-indica', 'El nopal de toda la vida, resistente y comestible.',
  2900.00, 21, '/images/placeholder-planta.svg', NOW(),
  3, 'MEDIANO', 'SOL_DIRECTO', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'EXTERIOR', 'SECO', 'PRINCIPIANTE', 'CACTUS', 0, 'seed', NOW()),
 (27, 'Cola de mono', 'Cleistocactus winteri', 'Custodia dorada que cuelga en cascada, flores rosadas.',
  5200.00, 10, '/images/placeholder-planta.svg', NOW(),
  3, 'MEDIANO', 'MEDIA_SOMBRA', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'INTERMEDIO', 'CACTUS', 0, 'seed', NOW()),
 (28, 'Barril de oro', 'Echinocactus grusonii', 'El clásico cojín sedoso de espinas doradas.',
  7200.00, 8, '/images/placeholder-planta.svg', NOW(),
  3, 'MEDIANO', 'SOL_DIRECTO', 'BARRO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'EXTERIOR', 'SECO', 'PRINCIPIANTE', 'CACTUS', 0, 'seed', NOW()),
 (29, 'Gymnocalycium', 'Gymnocalycium mihanovichii', 'Pequeño cactus de flor abundante y colorida.',
  3100.00, 16, '/images/placeholder-planta.svg', NOW(),
  3, 'PEQUENO', 'MEDIA_SOMBRA', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'CACTUS', 0, 'seed', NOW()),
 (30, 'Opuntia microdasys', 'Opuntia microdasys', 'Palas cubiertas de gloquidios amarillos, muy decorativa.',
  2700.00, 24, '/images/placeholder-planta.svg', NOW(),
  3, 'PEQUENO', 'SOL_DIRECTO', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'CACTUS', 0, 'seed', NOW()),
-- Suculentas
 (31, 'Echeveria', 'Echeveria elegans', 'Suculenta de roseta compacta, perfecta para maceteros pequeños.',
  1800.00, 40, '/images/placeholder-planta.svg', NOW(),
  3, 'PEQUENO', 'SOL_DIRECTO', 'BARRO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
 (32, 'Aloe vera', 'Aloe barbadensis', 'Suculenta medicinal, su gel calma quemaduras e irritaciones.',
  3600.00, 18, '/images/placeholder-planta.svg', NOW(),
  3, 'MEDIANO', 'SOL_DIRECTO', 'BARRO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'EXTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
 (33, 'Sedum burrito', 'Sedum morganianum', 'Colgante de cuentas verdes azuladas que enamoran.',
  4200.00, 14, '/images/placeholder-planta.svg', NOW(),
  3, 'MEDIANO', 'MEDIA_SOMBRA', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
 (34, 'Haworthia', 'Haworthia fasciata', 'Rayas blancas horizontales, ideal escritorios y repisas.',
  2300.00, 22, '/images/placeholder-planta.svg', NOW(),
  3, 'PEQUENO', 'MEDIA_SOMBRA', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
 (35, 'Árbol de jade', 'Crassula ovata', 'Porte de pequeño arbolito, hojas carnosas y brillantes.',
  6800.00, 9, '/images/placeholder-planta.svg', NOW(),
  3, 'MEDIANO', 'SOL_DIRECTO', 'BARRO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
 (36, 'Pachyphytum oviferum', 'Pachyphytum oviferum', 'Hojitas redondeadas blanquecinas como confites de luna.',
  2900.00, 17, '/images/placeholder-planta.svg', NOW(),
  3, 'PEQUENO', 'SOL_DIRECTO', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
 (37, 'Crásula perforata', 'Crassula perforata', 'Hojas en cadena superpuestas, de estética geométrica.',
  2100.00, 26, '/images/placeholder-planta.svg', NOW(),
  3, 'PEQUENO', 'MEDIA_SOMBRA', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
 (38, 'Kleinia articulata', 'Kleinia articulata', 'Tubercada y colgante, estética muy original.',
  3100.00, 11, '/images/placeholder-planta.svg', NOW(),
  3, 'MEDIANO', 'MEDIA_SOMBRA', 'PLASTICO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'INTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 0, 'seed', NOW()),
-- Flores, aromáticas, helechos, arboles, palmeras, enredaderas
 (39, 'Petunia', 'Petunia hybrida', 'Manto de flores de colores para canteros y macetas.',
  1500.00, 35, '/images/placeholder-planta.svg', NOW(),
  2, 'PEQUENO', 'SOL_DIRECTO', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'FLOR', 0, 'seed', NOW()),
 (40, 'Caléndula', 'Calendula officinalis', 'Flor comestible y medicinal, alegra cualquier cantero.',
  1700.00, 22, '/images/placeholder-planta.svg', NOW(),
  2, 'PEQUENO', 'SOL_DIRECTO', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'FLOR', 0, 'seed', NOW()),
 (41, 'Phalaenopsis', 'Phalaenopsis amabilis', 'Orquídea de flores blancas de larga duración.',
  9500.00, 8, '/images/placeholder-planta.svg', NOW(),
  4, 'MEDIANO', 'MEDIA_SOMBRA', 'CERAMICA', 'DRENAJE_ALTO', NULL, 2, 1, 'INTERIOR', 'HUMEDO', 'INTERMEDIO', 'ORQUIDEA', 0, 'seed', NOW()),
 (42, 'Dendrobium', 'Dendrobium nobile', 'Orquídea de cañas carnosas y flores gepegas.',
  8900.00, 7, '/images/placeholder-planta.svg', NOW(),
  4, 'MEDIANO', 'MEDIA_SOMBRA', 'CERAMICA', 'DRENAJE_ALTO', NULL, 2, 1, 'INTERIOR', 'TEMPLADO', 'EXPERTO', 'ORQUIDEA', 0, 'seed', NOW()),
 (43, 'Jazmín de invierno', 'Jasminum nudiflorum', 'Enredadera de floración temprana, perfumada.',
  6400.00, 9, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'SOL_DIRECTO', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'INTERMEDIO', 'ENREDADERA', 0, 'seed', NOW()),
 (44, 'Menta', 'Mentha spicata', 'Aromática refrescante que crece sin parar.',
  1300.00, 30, '/images/placeholder-planta.svg', NOW(),
  2, 'PEQUENO', 'MEDIA_SOMBRA', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'AROMATICA', 0, 'seed', NOW()),
 (45, 'Romero', 'Rosmarinus officinalis', 'Aromática mediterránea, ideal para carne y asados.',
  1900.00, 25, '/images/placeholder-planta.svg', NOW(),
  2, 'PEQUENO', 'SOL_DIRECTO', 'BARRO', 'DRENAJE_ALTO', NULL, 2, 1, 'EXTERIOR', 'SECO', 'PRINCIPIANTE', 'AROMATICA', 0, 'seed', NOW()),
 (46, 'Albahaca', 'Ocimum basilicum', 'La reina de la cocina, tapa los cereales de la huerta.',
  1200.00, 28, '/images/placeholder-planta.svg', NOW(),
  5, 'PEQUENO', 'SOL_DIRECTO', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'AROMATICA', 0, 'seed', NOW()),
 (47, 'Olivo', 'Olea europaea', 'Árbol milenario de hojas plateadas, símbolo de paz.',
  22000.00, 4, '/images/placeholder-planta.svg', NOW(),
  2, 'GIGANTE', 'SOL_DIRECTO', 'BARRO', 'DRENAJE_ALTO', NULL, 2, 1, 'EXTERIOR', 'SECO', 'INTERMEDIO', 'ARBOL', 0, 'seed', NOW()),
 (48, 'Chamaedorea', 'Chamaedorea elegans', 'Palmerita de salón elegante y de bajo mantenimiento.',
  7400.00, 13, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'MEDIA_SOMBRA', 'CERAMICA', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'PALMERA', 0, 'seed', NOW()),
 (49, 'Culantrillo', 'Adiantum raddianum', 'Helecho fino de película, delicioso para rincones húmedos.',
  4300.00, 10, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'SOMBRA', 'BARRO', 'TURBA_PERLITA', NULL, 2, 1, 'INTERIOR', 'HUMEDO', 'EXPERTO', 'HELECHO', 0, 'seed', NOW()),
 (50, 'Helecho cuero', 'Rumohra adiantiformis', 'Follaje de follaje que conserva su color todo el año.',
  5100.00, 9, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'MEDIA_SOMBRA', 'PLASTICO', 'TURBA_PERLITA', NULL, 2, 1, 'INTERIOR', 'HUMEDO', 'INTERMEDIO', 'HELECHO', 0, 'seed', NOW()),
-- Macetas / Soportes / Sustratos
 (51, 'Maceta de barro 20cm', 'Terracota artesanal', 'Maceta de barro cocido con buen drenaje, porosa e ideal para suculentas.',
  2500.00, 50, '/images/placeholder-planta.svg', NOW(),
  6, 'MEDIANO', 'MEDIA_SOMBRA', 'BARRO', NULL, NULL, 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (52, 'Maceta cerámica esmaltada', 'Cerámica de autor', 'Macetero decorativo esmaltado con plato a juego.',
  5800.00, 30, '/images/placeholder-planta.svg', NOW(),
  6, 'MEDIANO', 'MEDIA_SOMBRA', 'CERAMICA', NULL, NULL, 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (53, 'Maceta autorriego 25cm', 'Plástico con reservorio', 'Regá menos: el reservorio hidrata la raíz sola.',
  6900.00, 22, '/images/placeholder-planta.svg', NOW(),
  6, 'MEDIANO', 'MEDIA_SOMBRA', 'AUTORRIEGO', NULL, NULL, 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (54, 'Tutores de bambú x5', 'Bambú natural', 'Tutores livianos para guiar tallos y enredaderas.',
  1900.00, 60, '/images/placeholder-planta.svg', NOW(),
  7, 'MEDIANO', 'MEDIA_SOMBRA', NULL, NULL, 'TUTOR', 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (55, 'Estaca espiral para enredaderas', 'Metal recubierto', 'Soporte en espiral que acompaña el crecimiento de las trepadoras.',
  3400.00, 25, '/images/placeholder-planta.svg', NOW(),
  7, 'MEDIANO', 'MEDIA_SOMBRA', NULL, NULL, 'TUTOR', 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (56, 'Sustrato universal 8L', 'Mezcla balanceada', 'Base ideal para la mayoría de las plantas de interior y exterior.',
  2800.00, 40, '/images/placeholder-planta.svg', NOW(),
  8, 'MEDIANO', 'MEDIA_SOMBRA', NULL, 'UNIVERSAL', NULL, 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (57, 'Sustrato cactus y suculentas 5L', 'Mezcla árida', 'Con arena gruesa y drenaje excelente para plantas áridas.',
  3500.00, 35, '/images/placeholder-planta.svg', NOW(),
  8, 'MEDIANO', 'MEDIA_SOMBRA', NULL, 'CACTUS_SUCULENTAS', NULL, 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (58, 'Perlita drenaje 5L', 'Mineral ligero', 'Aireá el sustrato y evitá el encharcamiento de raíces.',
  2200.00, 38, '/images/placeholder-planta.svg', NOW(),
  8, 'MEDIANO', 'MEDIA_SOMBRA', NULL, 'DRENAJE_ALTO', NULL, 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
-- Combos
 (59, 'Combo Iniciación', 'Kit Potus + maceta + sustrato', 'Todo lo que necesitás para empezar: planta fácil, maceta y sustrato.',
  8500.00, 12, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'MEDIA_SOMBRA', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'INTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'ENREDADERA', 1, 'seed', NOW()),
 (60, 'Combo Desierto', '3 suculentas + maceta de barro', 'Selección de tres suculentas con maceta de barro incluida.',
  9900.00, 8, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'SOL_DIRECTO', 'BARRO', 'CACTUS_SUCULENTAS', NULL, 2, 1, 'EXTERIOR', 'SECO', 'PRINCIPIANTE', 'SUCULENTA', 1, 'seed', NOW()),
 (61, 'Combo Selva Húmeda', 'Monstera + helecho + maceta cerámica', 'Un rincón de jungla con dos plantas y su maceta decorativa.',
  14500.00, 6, '/images/placeholder-planta.svg', NOW(),
  5, 'GRANDE', 'MEDIA_SOMBRA', 'CERAMICA', 'TURBA_PERLITA', NULL, 2, 1, 'INTERIOR', 'HUMEDO', 'INTERMEDIO', 'TROPICAL', 1, 'seed', NOW()),
 (62, 'Combo Aromáticas', 'Romero + menta + albahaca + maceta', 'Cuatro aromáticas listas para la cocina en una sola caja.',
  6500.00, 10, '/images/placeholder-planta.svg', NOW(),
  5, 'MEDIANO', 'SOL_DIRECTO', 'PLASTICO', 'UNIVERSAL', NULL, 2, 1, 'EXTERIOR', 'TEMPLADO', 'PRINCIPIANTE', 'AROMATICA', 1, 'seed', NOW()),
 (63, 'Mesa de hierro para macetas', 'Hierro pintado', 'Base de hierro de 60cm con apoyos antideslizantes para destacar tus macetas.',
  12500.00, 15, '/images/placeholder-planta.svg', NOW(),
  7, 'GRANDE', 'MEDIA_SOMBRA', NULL, NULL, 'MESA', 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (64, 'Colgante macramé', 'Fibras naturales', 'Soporte colgante tejido a mano para colgar tus plantas cerca de la luz.',
  8900.00, 20, '/images/placeholder-planta.svg', NOW(),
  7, 'MEDIANO', 'MEDIA_SOMBRA', NULL, NULL, 'COLGANTE', 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW()),
 (65, 'Estante flotante para macetas', 'Madera maciza', 'Estante de madera para dar niveles a tus plantas y ganar espacio.',
  10500.00, 18, '/images/placeholder-planta.svg', NOW(),
  7, 'MEDIANO', 'MEDIA_SOMBRA', NULL, NULL, 'ESTANTE', 2, 1, NULL, NULL, NULL, NULL, 0, 'seed', NOW());

-- Guías de cuidado (asociadas a categorías genéricas y a una planta puntual)
INSERT INTO guias_cuidado (id, titulo_ficha, planta_id, categoria_generica, frecuencia_riego, humedad, temperatura, recomendaciones, activo, created_by, created_at) VALUES
 (1, 'Cuidados del Potus', 1, NULL, 'Cada 7-10 días, dejar secar el sustrato entre riegos',
  'Media (40-60%)', '18-27°C', 'Tolera poca luz. Podar guías largas para mantener forma tupida.', 1, 'seed', NOW()),
 (2, 'Cuidado general de Suculentas', NULL, 'Suculentas', 'Cada 15-20 días en verano, menos en invierno',
  'Baja', '15-30°C', 'Requieren sustrato con muy buen drenaje y sol directo varias horas al día.', 1, 'seed', NOW()),
 (3, 'Cuidado general de Orquídeas', NULL, 'Orquídeas', 'Cada 7 días, riego por inmersión breve',
  'Alta (60-80%)', '18-25°C', 'Evitar encharcamiento de raíces. Preferir luz indirecta intensa.', 1, 'seed', NOW());

-- Blog: 3 publicaciones
INSERT INTO blog_posts (id, titulo, slug, contenido, autor_id, fecha, categoria, activo, created_by, created_at) VALUES
 (1, '5 plantas ideales para principiantes', '5-plantas-ideales-para-principiantes',
  'Si estás empezando en el mundo de las plantas, lo mejor es arrancar con especies resistentes y de bajo mantenimiento como el Potus, la Sansevieria, el Zamioculca, los Pothos y las suculentas. Estas plantas toleran errores de riego ocasionales y se adaptan bien a distintos niveles de luz, lo que las convierte en la puerta de entrada perfecta al mundo de la jardinería de interior.',
  1, NOW(), 'Consejos', 1, 'seed', NOW()),
 (2, 'Cómo elegir la maceta correcta para cada planta', 'como-elegir-la-maceta-correcta',
  'La elección de la maceta influye directamente en la salud de la raíz. Las macetas de barro son porosas y favorecen el secado del sustrato, ideales para suculentas y cactus. Las de plástico retienen más humedad y son livianas, útiles para plantas tropicales de interior. Las de cerámica esmaltada son decorativas pero conviene usarlas como macetero exterior sobre una maceta interna con buen drenaje.',
  1, NOW(), 'Guías', 1, 'seed', NOW()),
 (3, 'Errores comunes al regar tus plantas', 'errores-comunes-al-regar-tus-plantas',
  'El exceso de riego es la causa número uno de muerte en plantas de interior. Antes de regar, comprobá la humedad del sustrato con el dedo a unos 3 centímetros de profundidad. Preferí macetas con orificios de drenaje y platos que permitan retirar el agua sobrante. Cada planta tiene su propio ritmo: informate sobre la especie puntual antes de establecer una rutina fija de riego.',
  1, NOW(), 'Consejos', 1, 'seed', NOW());

-- Contenido dinámico del sitio (Home y Términos)
INSERT INTO site_config (id, clave, valor, created_by, created_at) VALUES
 (1, 'HOME_TITLE', 'Bienvenido a Mendo Plant', 'seed', NOW()),
 (2, 'HOME_SUBTITLE', 'El marketplace de plantas para amantes del verde', 'seed', NOW()),
 (3, 'HOME_BANNER', 'Encontrá la planta ideal para cada rincón de tu casa, publicá las tuyas y sumate a nuestra comunidad de plant lovers.', 'seed', NOW()),
 (4, 'TERMS_CONDITIONS', 'Al utilizar Mendo Plant aceptás que la compra de plantas es un proceso simulado con fines demostrativos. Los vendedores son responsables de la descripción y el estado de las plantas publicadas. Mendo Plant se reserva el derecho de moderar contenido y suspender cuentas que incumplan las normas de la comunidad.', 'seed', NOW());

-- ---------------------------------------------------------------------
-- Promociones (descuentos por porcentaje, configurables desde admin)
--   "Llegada de la primavera": 30% en plantas de clima TROPICAL, todos los
--   días, vigente del 21/09 al 21/12 (antes estaba hardcodeada en código).
-- ---------------------------------------------------------------------
INSERT INTO promociones (id, nombre, porcentaje, dias_semana, fecha_inicio, fecha_fin, tipo_aplicacion, categoria_id, clima, activa, created_by, created_at) VALUES
 (1, 'Llegada de la primavera', 30.00, '', '2026-09-21', '2026-12-21', 'TODAS', NULL, 'TROPICAL', 1, 'seed', NOW());

-- ---------------------------------------------------------------------
-- Ofertas del día: una planta destacada por día de la semana (rotativa).
-- ISO day: 1=lunes ... 7=domingo. El admin puede cambiarlas cuando quiera.
-- ---------------------------------------------------------------------
INSERT INTO ofertas_dia (id, dia_semana, planta_id, porcentaje, activa, created_by, created_at) VALUES
 (1, 1, 3, 30.00, 1, 'seed', NOW()),   -- Lunes: Monstera -30%
 (2, 2, 6, 20.00, 1, 'seed', NOW()),   -- Martes: Palmera Areca -20%
 (3, 3, 9, 25.00, 1, 'seed', NOW()),   -- Miércoles: Helecho de Boston -25%
 (4, 4, 16, 25.00, 1, 'seed', NOW()),  -- Jueves: Palmera Phoenix -25%
 (5, 5, 39, 20.00, 1, 'seed', NOW()),  -- Viernes: Petunia -20%
 (6, 6, 28, 30.00, 1, 'seed', NOW()),  -- Sábado: Barril de oro -30%
 (7, 7, 41, 20.00, 1, 'seed', NOW());  -- Domingo: Phalaenopsis -20%

-- Reiniciar los auto-incrementos para evitar colisiones con nuevos registros
ALTER TABLE usuarios      AUTO_INCREMENT = 3;
ALTER TABLE categorias    AUTO_INCREMENT = 9;
ALTER TABLE plantas       AUTO_INCREMENT = 66;
ALTER TABLE guias_cuidado AUTO_INCREMENT = 4;
ALTER TABLE blog_posts    AUTO_INCREMENT = 4;
ALTER TABLE site_config   AUTO_INCREMENT = 5;
ALTER TABLE promociones   AUTO_INCREMENT = 2;
ALTER TABLE ofertas_dia   AUTO_INCREMENT = 8;