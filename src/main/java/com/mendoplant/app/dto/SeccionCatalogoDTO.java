package com.mendoplant.app.dto;

import com.mendoplant.app.model.Plant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Sección de la portada con su título y sus plantas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeccionCatalogoDTO {
    private String titulo;
    private List<Plant> plantas;
}