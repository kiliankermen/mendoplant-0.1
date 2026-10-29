package com.mendoplant.app.service;

import com.mendoplant.app.dto.BlogPostDTO;
import com.mendoplant.app.model.BlogPost;
import com.mendoplant.app.model.CareGuide;

import java.util.List;

public interface BlogService {
    List<BlogPost> listarTodos();
    List<BlogPost> listarPublicados();
    BlogPost buscarPorSlug(String slug);
    BlogPost buscarPorId(Long id);
    BlogPost crear(BlogPostDTO dto, Long autorId);
    BlogPost actualizar(Long id, BlogPostDTO dto);
    void eliminar(Long id);

    List<CareGuide> listarGuias();
    List<CareGuide> listarGuiasActivas();
    CareGuide guardarGuia(CareGuide guia);
    void eliminarGuia(Long id);
}
