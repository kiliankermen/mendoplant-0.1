package com.mendoplant.app.config;

import com.mendoplant.app.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expone la carpeta de imágenes subidas (./uploads) como recursos públicos {@code /uploads/**}.
 * Además, registra el filtro de multipart ANTES de Spring Security para que el token CSRF
 * pueda leerse en formularios con {@code enctype="multipart/form-data"} (subida de imágenes).
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ImageStorageService imageStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(imageStorageService.getRaiz().toUri().toString());
    }

    /** Resolver de multipart usado por {@link MultipartFilter} (se ejecuta antes del filtro de seguridad). */
    @Bean(name = "filterMultipartResolver")
    public MultipartResolver filterMultipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilterRegistration() {
        FilterRegistrationBean<MultipartFilter> registro = new FilterRegistrationBean<>(new MultipartFilter());
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registro.addUrlPatterns("/dashboard/publicar-*", "/plantas/editar/*");
        return registro;
    }
}