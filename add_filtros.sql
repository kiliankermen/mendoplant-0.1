-- Ajustes para modo excluyente: agregar ambas etiquetas cuando tiene sentido
-- Ambientes: plantas versátiles pueden ser INTERIOR y EXTERIOR
INSERT IGNORE INTO plantas_ambiente (planta_id, valor) VALUES
(1,'EXTERIOR'),  -- Potus
(2,'EXTERIOR'),  -- Sansevieria
(3,'EXTERIOR'),  -- Monstera
(4,'EXTERIOR'),  -- Pilea peperomioides
(5,'EXTERIOR'),  -- Zamioculca
(6,'EXTERIOR'),  -- Palmera areca
(7,'EXTERIOR'),  -- Cinta o malamadre
(8,'EXTERIOR'),  -- Calathea
(9,'EXTERIOR'),  -- Helecho de Boston
(10,'EXTERIOR'), -- Ficus benjamina
(11,'EXTERIOR'), -- Ficus lyrata
(12,'EXTERIOR'), -- Crassula ovata
(13,'EXTERIOR'), -- Echeveria elegans
(14,'EXTERIOR'), -- Aloe vera
(15,'EXTERIOR'), -- Haworthia fasciata
(16,'EXTERIOR'), -- Palmera Phoenix
(17,'EXTERIOR'), -- Hiedra inglesa
(18,'EXTERIOR'), -- Aspidistra
(19,'EXTERIOR'), -- Maranta leuconeura
(20,'EXTERIOR'), -- Peperomia obtusifolia
(21,'EXTERIOR'), -- Aglaonema
(22,'EXTERIOR'), -- Tradescantia zebrina
(23,'EXTERIOR'), -- Begonia rex
(24,'EXTERIOR'), -- Fittonia
(25,'EXTERIOR'), -- Nephrolepis exaltata
(26,'EXTERIOR'), -- Saintpaulia ionantha
(27,'EXTERIOR'), -- Chlorophytum comosum
(28,'EXTERIOR'), -- Pachypodium lamerei
(29,'EXTERIOR'), -- Schefflera arboricola
(30,'EXTERIOR'), -- Yucca elephantipes
(31,'EXTERIOR'), -- Dracaena marginata
(32,'EXTERIOR'), -- Syngonium podophyllum
(33,'EXTERIOR'), -- Philodendron hederaceum
(34,'EXTERIOR'), -- Epipremnum aureum
(35,'EXTERIOR'), -- Kalanchoe blossfeldiana
(36,'EXTERIOR'), -- Sedum morganianum
(37,'EXTERIOR'), -- Graptopetalum paraguayense
(38,'EXTERIOR'), -- Geranio
(39,'EXTERIOR'), -- Petunia
(40,'EXTERIOR'), -- Lavanda
(41,'EXTERIOR'), -- Orquídea Phalaenopsis
(42,'EXTERIOR'); -- Anthurium

-- Climas versátiles
INSERT IGNORE INTO plantas_clima (planta_id, valor) VALUES
(1,'SECO'),
(2,'HUMEDO'),
(3,'SECO'),
(5,'HUMEDO'),
(6,'SECO'),
(9,'SECO'),
(12,'HUMEDO'),
(14,'HUMEDO'),
(16,'SECO'),
(17,'HUMEDO'),
(27,'SECO'),
(28,'HUMEDO'),
(31,'SECO'),
(34,'HUMEDO'),
(35,'SECO'),
(38,'SECO'),
(39,'HUMEDO'),
(40,'HUMEDO');
