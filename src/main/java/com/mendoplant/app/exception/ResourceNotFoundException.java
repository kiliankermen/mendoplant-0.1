package com.mendoplant.app.exception;

/** Se lanza cuando una entidad solicitada no existe (planta, usuario, orden, etc). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
