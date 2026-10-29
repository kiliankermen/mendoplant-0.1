package com.mendoplant.app.repository;

import com.mendoplant.app.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlug(String slug);
    List<BlogPost> findAllByOrderByFechaDesc();

    /** Posts publicados (no eliminados lógicamente) para el blog público. */
    Optional<BlogPost> findBySlugAndActivoTrue(String slug);
    List<BlogPost> findByActivoTrueOrderByFechaDesc();
}
