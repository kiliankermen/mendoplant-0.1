package com.mendoplant.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Datos de envío capturados en el checkout: dirección de entrega del comprador.
 */
@Data
public class EnvioDTO {

    @NotBlank(message = "El nombre de quien recibe es obligatorio")
    @Size(max = 120)
    private String receptorNombre;

    @Size(max = 30)
    private String telefono;

    @NotBlank(message = "La calle y número son obligatorios")
    @Size(max = 200)
    private String calleNumero;

    @Size(max = 50)
    private String depto;

    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 80)
    private String ciudad;

    @Size(max = 12)
    private String codigoPostal;

    @NotBlank(message = "La provincia es obligatoria")
    @Size(max = 60)
    private String provincia;

    @Size(max = 300)
    private String instrucciones;
}