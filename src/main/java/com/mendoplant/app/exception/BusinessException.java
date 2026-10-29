package com.mendoplant.app.exception;

/** Se lanza ante una violación de una regla de negocio (stock insuficiente, email duplicado, etc). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
