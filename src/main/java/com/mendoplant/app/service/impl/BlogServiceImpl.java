package com.mendoplant.app.service.impl;

import com.mendoplant.app.dto.BlogPostDTO;
import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.BlogPost;
import com.mendoplant.app.model.CareGuide;
import com.mendoplant.app.model.User;
import com.mendoplant.app.repository.BlogPostRepository;
import com.mendoplant.app.repository.CareGuideRepository;
import com.mendoplant.app.repository.UserRepository;
import com.mendoplant.app.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogPostRepository blogPostRepository;
    private final CareGuideRepository careGuideRepository;
    private final UserRepository userRepository;

    @Override
    public List<BlogPost> listarTodos() {
        return blogPostRepository.findAllByOrderByFechaDesc();
    }

    /** Posts publicados (activos) para el blog público. */
    @Override
    public List<BlogPost> listarPublicados() {
        return blogPostRepository.findByActivoTrueOrderByFechaDesc();
    }

    @Override
    public BlogPost buscarPorSlug(String slug) {
        return blogPostRepository.findBySlugAndActivoTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado: " + slug));
    }

    @Override
    public BlogPost buscarPorId(Long id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con id: " + id));
    }

    @Override
    @Transactional
    public BlogPost crear(BlogPostDTO dto, Long autorId) {
        User autor = userRepository.findById(autorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + autorId));

        String slug = generarSlugUnico(dto.getTitulo());

        BlogPost post = BlogPost.builder()
                .titulo(dto.getTitulo())
                .slug(slug)
                .contenido(dto.getContenido())
                .categoria(dto.getCategoria())
                .autor(autor)
                .build();
        return blogPostRepository.save(post);
    }

    @Override
    @Transactional
    public BlogPost actualizar(Long id, BlogPostDTO dto) {
        BlogPost post = buscarPorId(id);
        post.setTitulo(dto.getTitulo());
        post.setContenido(dto.getContenido());
        post.setCategoria(dto.getCategoria());
        return blogPostRepository.save(post);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con id: " + id));
        // Eliminación lógica: el artículo se oculta del blog público.
        post.setActivo(false);
        blogPostRepository.save(post);
    }

    @Override
    public List<CareGuide> listarGuias() {
        return careGuideRepository.findAll();
    }

    /** Guías de cuidado vigentes (activas) para la sección pública. */
    @Override
    public List<CareGuide> listarGuiasActivas() {
        return careGuideRepository.findByActivoTrueOrderByTituloFichaAsc();
    }

    @Override
    @Transactional
    public CareGuide guardarGuia(CareGuide guia) {
        if (guia.getTituloFicha() == null || guia.getTituloFicha().isBlank()) {
            throw new BusinessException("El título de la ficha de cuidado es obligatorio");
        }
        return careGuideRepository.save(guia);
    }

    @Override
    @Transactional
    public void eliminarGuia(Long id) {
        CareGuide guia = careGuideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de cuidado no encontrada con id: " + id));
        guia.setActivo(false);
        careGuideRepository.save(guia);
    }

    private String generarSlugUnico(String titulo) {
        String base = toSlug(titulo);
        String slug = base;
        int contador = 1;
        while (blogPostRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + contador++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        String slug = withoutAccents.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        return slug.isBlank() ? "articulo" : slug;
    }
}
