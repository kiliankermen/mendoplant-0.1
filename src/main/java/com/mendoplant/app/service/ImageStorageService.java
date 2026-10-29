package com.mendoplant.app.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Guarda las imágenes subidas por los usuarios dentro de la carpeta del proyecto
 * ({@code ./uploads}) y las expone públicamente bajo {@code /uploads/**}.
 */
@Service
public class ImageStorageService {

    private static final String CARPETA_AVATARES = "users-profile-photos";

    private final Path raiz;

    @Autowired
    private ApplicationContext applicationContext;

    /** Imágenes colocadas manualmente en uploads/ con nombre = id de planta (ej.: 39.jpg o 39.0.jpg) o por nombre científico (ej.: phoenix_canariensis_0.jpg). */
    private final Map<Long, String> imagenesPorId = new HashMap<>();

    /** Imagen alternativa para el hover (ej.: 39.1.jpg). */
    private final Map<Long, String> imagenesHoverPorId = new HashMap<>();

    /** Mapa nombre científico normalizado -> imagen base. */
    private final Map<String, String> imagenesPorNombreCientifico = new HashMap<>();

    /** Mapa nombre científico normalizado -> imagen hover. */
    private final Map<String, String> imagenesHoverPorNombreCientifico = new HashMap<>();

    /** Momento del último escaneo de la carpeta de subidas (para detectar archivos nuevos). */
    private volatile long ultimoEscaneo = 0L;

    public ImageStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        Path base = Path.of(uploadDir);
        this.raiz = (base.isAbsolute() ? base : Path.of(System.getProperty("user.dir")).resolve(base)).normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(raiz);
        escanearImagenes();
    }

    /**
     * Escanea la carpeta de subidas en busca de archivos nombrados con el id de la planta
     * (ej. {@code 39.jpg}) y guarda su URL pública {@code /uploads/{id}{ext}}.
     */
    private synchronized void escanearImagenes() {
        imagenesPorId.clear();
        imagenesHoverPorId.clear();
        imagenesPorNombreCientifico.clear();
        imagenesHoverPorNombreCientifico.clear();
        escanearDirectorio(raiz, "");
        Path plantasDir = raiz.resolve("plants");
        if (Files.isDirectory(plantasDir)) {
            escanearDirectorio(plantasDir, "plants/");
        }
        ultimoEscaneo = System.currentTimeMillis();
    }

    private void escanearDirectorio(Path directorio, String prefijoUrl) {
        try (Stream<Path> stream = Files.list(directorio)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        String nombre = p.getFileName().toString();
                        int i = nombre.lastIndexOf('.');
                        if (i <= 0) {
                            return;
                        }
                        String base = nombre.substring(0, i);
                        String[] stem = base.split("\\.", -1);
                        // Por id de planta: 39.jpg, 39.1.jpg
                        if (stem.length <= 2 && stem[0].matches("\\d+")
                                && (stem.length == 1 || stem[1].matches("\\d+"))) {
                            long id;
                            try {
                                id = Long.parseLong(stem[0]);
                            } catch (NumberFormatException e) {
                                return;
                            }
                            String variante = stem.length == 2 ? stem[1] : "";
                            String url = "/uploads/" + prefijoUrl + nombre;
                            if ("1".equals(variante)) {
                                imagenesHoverPorId.put(id, url);
                            } else if (variante.isEmpty() || "0".equals(variante)) {
                                imagenesPorId.put(id, url);
                            }
                            return;
                        }
                        // Por nombre científico: phoenix_canariensis_0.jpg, nephrolepis_exaltata_1.jpg
                        if (base.matches("[a-z0-9_]+(_[0-9]+)?")) {
                            String[] partes = base.split("_");
                            if (partes.length < 2) {
                                return;
                            }
                            String ultimo = partes[partes.length - 1];
                            String clave;
                            String variante;
                            if (ultimo.matches("[0-9]+")) {
                                clave = String.join("_", Arrays.copyOf(partes, partes.length - 1));
                                variante = ultimo;
                            } else {
                                clave = base;
                                variante = "0";
                            }
                            String url = "/uploads/" + prefijoUrl + nombre;
                            if ("1".equals(variante)) {
                                imagenesHoverPorNombreCientifico.put(clave, url);
                            } else if (variante.isEmpty() || "0".equals(variante)) {
                                imagenesPorNombreCientifico.put(clave, url);
                            }
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private synchronized void asegurarEscaneado() {
        long ahora = System.currentTimeMillis();
        if (imagenesPorId.isEmpty() || ahora - ultimoEscaneo > 5000L) {
            escanearImagenes();
        }
    }

    /** URL de la imagen subida a mano para la planta (por id o por nombre científico), o null si no existe. */
    public String urlPlanta(Long id) {
        if (id == null) {
            return null;
        }
        asegurarEscaneado();
        String porId = imagenesPorId.get(id);
        if (porId != null) {
            return porId;
        }
        return null;
    }

    /** URL de la imagen de hover ({@code /uploads/{id}.1.jpg}) o null si no existe. */
    public String urlPlantaHover(Long id) {
        if (id == null) {
            return null;
        }
        asegurarEscaneado();
        String porId = imagenesHoverPorId.get(id);
        if (porId != null) {
            return porId;
        }
        return null;
    }

    /**
     * URL final que se muestra para una planta: primero la imagen subida con el número del id
     * ({@code /uploads/39.jpg}), o por nombre científico, luego la guardada en la base,
     * y como respaldo el marcador local.
     */
    public String imagenPlanta(Long id, String imagenUrl) {
        String porId = urlPlanta(id);
        if (porId != null) {
            return porId;
        }
        String porNombre = urlPlantaPorNombre(id);
        if (porNombre != null) {
            return porNombre;
        }
        return (imagenUrl != null && !imagenUrl.isBlank()) ? imagenUrl
                : "/images/placeholder-planta.svg";
    }

    /**
     * URL final para la imagen de hover de una planta: primero la subida con el número del id
     * ({@code /uploads/39.1.jpg}) o la de hover por nombre; como respaldo la imagen base;
     * {@code null} si no hay nada.
     */
    public String imagenPlantaHover(Long id, String imagenUrl) {
        String hover = urlPlantaHover(id);
        if (hover != null) {
            return hover;
        }
        String hoverNombre = urlPlantaHoverPorNombre(id);
        if (hoverNombre != null) {
            return hoverNombre;
        }
        String base = urlPlanta(id);
        if (base != null) {
            return base;
        }
        String baseNombre = urlPlantaPorNombre(id);
        if (baseNombre != null) {
            return baseNombre;
        }
        return (imagenUrl != null && !imagenUrl.isBlank()) ? imagenUrl : null;
    }

    /** Claves normalizadas con las que se buscan imágenes de una planta: nombre científico y nombre común. */
    private List<String> clavesImagen(Long id, String nombreCientifico, String nombreComun) {
        LinkedHashSet<String> claves = new LinkedHashSet<>();
        if (nombreCientifico != null && !nombreCientifico.isBlank()) {
            claves.add(normalizar(nombreCientifico));
        }
        if (nombreComun != null && !nombreComun.isBlank()) {
            claves.add(normalizar(nombreComun));
        }
        if (id != null) {
            com.mendoplant.app.model.Plant planta = obtenerPlanta(id);
            if (planta != null) {
                if (planta.getNombreCientifico() != null) {
                    claves.add(normalizar(planta.getNombreCientifico()));
                }
                if (planta.getNombreComun() != null) {
                    claves.add(normalizar(planta.getNombreComun()));
                }
            }
        }
        claves.removeIf(k -> k == null || k.isBlank());
        return new ArrayList<>(claves);
    }

    private String urlPlantaPorNombre(Long id) {
        if (id == null) {
            return null;
        }
        asegurarEscaneado();
        for (String clave : clavesImagen(id, null, null)) {
            String url = imagenesPorNombreCientifico.get(clave);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private String urlPlantaHoverPorNombre(Long id) {
        if (id == null) {
            return null;
        }
        asegurarEscaneado();
        for (String clave : clavesImagen(id, null, null)) {
            String url = imagenesHoverPorNombreCientifico.get(clave);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private com.mendoplant.app.model.Plant obtenerPlanta(Long id) {
        try {
            if (applicationContext == null) {
                return null;
            }
            com.mendoplant.app.repository.PlantRepository repo = applicationContext.getBean(com.mendoplant.app.repository.PlantRepository.class);
            return repo.findById(id).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** Todas las imágenes de una planta (por id o por nombre científico/común): _0 primero, luego _1, etc. */
    public List<String> imagenesPlanta(Long id, String imagenUrl, String nombreCientifico) {
        return imagenesPlanta(id, imagenUrl, nombreCientifico, null);
    }

    /** Todas las imágenes de una planta (por id o por nombre científico/común): _0 primero, luego _1, etc. */
    public List<String> imagenesPlanta(Long id, String imagenUrl, String nombreCientifico, String nombreComun) {
        LinkedHashSet<String> imgs = new LinkedHashSet<>();
        asegurarEscaneado();
        com.mendoplant.app.model.Plant planta = obtenerPlanta(id);
        if (planta != null && planta.getImagenes() != null) {
            for (String guardada : planta.getImagenes()) {
                if (guardada != null && !guardada.isBlank()) {
                    imgs.add(guardada);
                }
            }
        }
        List<String> claves = clavesImagen(id, nombreCientifico, nombreComun);
        Path dir1 = raiz.resolve("plants");
        for (String clave : claves) {
            if (Files.isDirectory(dir1)) {
                agregarImagenesDeDirectorio(dir1, clave, imgs);
            }
            agregarImagenesDeDirectorio(raiz, clave, imgs);
        }
        if (id != null) {
            String stem = String.valueOf(id);
            if (Files.isDirectory(dir1)) {
                agregarImagenesPorStem(dir1, stem, imgs);
            }
            agregarImagenesPorStem(raiz, stem, imgs);
        }
        if (imagenUrl != null && !imagenUrl.isBlank()
                && !"/images/placeholder-planta.svg".equals(imagenUrl)
                && !imgs.contains(imagenUrl)) {
            imgs.add(imagenUrl);
        }
        if (imgs.isEmpty()) {
            String porId = urlPlanta(id);
            if (porId != null) {
                imgs.add(porId);
            }
            String porNombre = urlPlantaPorNombre(id);
            if (porNombre != null) {
                imgs.add(porNombre);
            }
        }
        if (imgs.isEmpty()) {
            imgs.add(imagenUrl != null && !imagenUrl.isBlank() ? imagenUrl : "/images/placeholder-planta.svg");
        }
        return new ArrayList<>(imgs);
    }

    private void agregarImagenesPorStem(Path dir, String stem, LinkedHashSet<String> imgs) {
        TreeMap<Integer, String> ordenadas = new TreeMap<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        String nombre = p.getFileName().toString();
                        int dot = nombre.lastIndexOf('.');
                        if (dot <= 0) {
                            return;
                        }
                        String base = nombre.substring(0, dot);
                        String[] parts = base.split("\\.", -1);
                        if (parts.length == 0 || !parts[0].equals(stem)) {
                            return;
                        }
                        String varianteStr = parts.length == 2 ? parts[1] : "";
                        if (varianteStr.matches("\\d+")) {
                            int variante = Integer.parseInt(varianteStr);
                            String url = dir.equals(raiz) ? "/uploads/" + nombre : "/uploads/plants/" + nombre;
                            ordenadas.put(variante, url);
                        }
                    });
        } catch (IOException ignored) {
        }
        imgs.addAll(ordenadas.values());
    }

    private void agregarImagenesDeDirectorio(Path dir, String clave, LinkedHashSet<String> imgs) {
        TreeMap<Integer, String> ordenadas = new TreeMap<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        String nombre = p.getFileName().toString();
                        int dot = nombre.lastIndexOf('.');
                        if (dot <= 0) {
                            return;
                        }
                        String base = nombre.substring(0, dot);
                        if (!base.matches("[a-z0-9_]+(_[0-9]+)?")) {
                            return;
                        }
                        String[] partes = base.split("_");
                        int variante = 0;
                        String ultimo = partes[partes.length - 1];
                        String clave2;
                        if (ultimo.matches("[0-9]+")) {
                            variante = Integer.parseInt(ultimo);
                            clave2 = String.join("_", Arrays.copyOf(partes, partes.length - 1));
                        } else {
                            clave2 = base;
                            variante = 0;
                        }
                        if (!clave2.equals(clave)) {
                            return;
                        }
                        String url = dir.equals(raiz) ? "/uploads/" + nombre : "/uploads/plants/" + nombre;
                        ordenadas.put(variante, url);
                    });
        } catch (IOException ignored) {
        }
        imgs.addAll(ordenadas.values());
    }

    private String normalizar(String nombreCientifico) {
        if (nombreCientifico == null) {
            return "";
        }
        return nombreCientifico.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    public Path getRaiz() {
        return raiz;
    }

    /**
     * Guarda un archivo de imagen y devuelve su URL pública ({@code /uploads/...}),
     * o {@code null} si el archivo es vacío. Lanza IllegalArgumentException si no es imagen.
     */
    public String guardar(MultipartFile archivo) {
        return guardarEn(archivo, raiz, "/uploads/", true);
    }

    /**
     * Guarda una foto de perfil en {@code uploads/users-profile-photos} y devuelve su URL pública
     * ({@code /uploads/users-profile-photos/...}).
     */
    public String guardarAvatar(MultipartFile archivo) {
        Path base = raiz.getFileName() != null && "users-profile-photos".equals(raiz.getFileName().toString()) ? raiz : raiz.resolve("users-profile-photos");
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo crear la carpeta de fotos de perfil.", e);
        }
        return guardarEn(archivo, base, "/uploads/users-profile-photos/", false);
    }

    private String guardarEn(MultipartFile archivo, Path directorio, String urlBase, boolean reescanear) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen (JPG, PNG, WebP, GIF o SVG).");
        }
        String nombre = UUID.randomUUID().toString().replace("-", "") + extensionDe(tipo, archivo.getOriginalFilename());
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, directorio.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo guardar la imagen.", e);
        }
        if (reescanear) {
            escanearImagenes();
        }
        return urlBase + nombre;
    }

    /** Elimina un archivo subido localmente (solo para URLs propias bajo /uploads/). */
    public void eliminar(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        try {
            Path destino = raiz.resolve(url.substring("/uploads/".length())).normalize();
            if (destino.startsWith(raiz)) {
                Files.deleteIfExists(destino);
                escanearImagenes();
            }
        } catch (IOException ignored) {
        }
    }

    private String extensionDe(String tipo, String nombreOriginal) {
        String extPorTipo = switch ((tipo == null ? "" : tipo).toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/svg+xml" -> ".svg";
            default -> "";
        };
        if (!extPorTipo.isEmpty()) {
            return extPorTipo;
        }
        if (nombreOriginal == null) {
            return ".jpg";
        }
        int i = nombreOriginal.lastIndexOf('.');
        if (i < 0 || i == nombreOriginal.length() - 1) {
            return ".jpg";
        }
        String ext = nombreOriginal.substring(i + 1).toLowerCase(Locale.ROOT);
        return (" .jpg .jpeg .png .webp .gif .svg .bmp").contains(" " + ext) ? "." + ext : ".jpg";
    }
}