package com.mendoplant.app.service;

/**
 * Verificación de email durante el registro con códigos de 6 dígitos.
 * La cuenta de usuario solo se crea cuando {@link #verificar} confirma un
 * código válido, lo que impide cuentas fantasmas con emails inexistentes.
 */
public interface VerificacionEmailService {

    int MINUTOS_VIGENCIA = 10;

    /**
     * Genera un código aleatorio de 6 dígitos, lo persiste con vigencia de 10
     * minutos y lo envía al email. Devuelve el código generado (útil en tests).
     */
    String generarYEnviarCodigo(String email);

    /**
     * Valida el código contra el más reciente sin usar del email.
     * Si es correcto y no expiró, lo marca como usado y devuelve {@code true}.
     * Un código usado, expirado o inexistente devuelve {@code false}.
     */
    boolean verificar(String email, String codigo);
}