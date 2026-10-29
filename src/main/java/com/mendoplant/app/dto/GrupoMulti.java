package com.mendoplant.app.dto;

import java.util.List;

/**
 * Un grupo de opciones del mismo atributo (ambiente, clima, luz, ...).
 * El "campo" es el nombre del parámetro que viaja en el formulario.
 */
public record GrupoMulti(String campo, String titulo, List<OpcionMulti> opciones) {
}
