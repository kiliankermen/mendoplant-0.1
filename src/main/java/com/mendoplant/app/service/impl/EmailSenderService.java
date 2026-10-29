package com.mendoplant.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío del código de verificación de email durante el registro.
 *
 * <p>Si SMTP está configurado ({@code MAIL_HOST}), el código se envía por email.
 * En caso contrario (desarrollo) NO se intenta ninguna conexión: el código se
 * imprime en la consola con un marcador fácil de buscar para poder probar el
 * flujo completo sin un servidor de correo real.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.from:Mendo Plant <no-responder@mendoplant.local>}")
    private String remitente;

    public boolean configurado() {
        return mailHost != null && !mailHost.trim().isEmpty();
    }

    /**
     * Envía (o loguea en modo desarrollo) el código de verificación de 6 dígitos al email.
     */
    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        String asunto = "Código de verificación - Mendo Plant";
        String cuerpo = """
                Hola!

                Tu código de verificación de Mendo Plant es:

                    %s

                Ingresalo en el sitio para confirmar tu cuenta. El código expira en 10 minutos.

                Si no estás creando una cuenta en Mendo Plant, ignorá este mensaje.
                """.formatted(codigo);

        if (!configurado()) {
            log.warn("[VERIFICACION] SMTP no configurado (MAIL_HOST vacío): NO se envía email. "
                    + "Código para '{}' = {}", destinatario, codigo);
            log.info("""
                    
                    ================================================
                      EMAIL DE VERIFICACIÓN (modo desarrollo)
                      Destinatario : {}
                      Código       : {}
                      Vigencia     : 10 minutos
                    ================================================
                    """, destinatario, codigo);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remitente);
        message.setTo(destinatario);
        message.setSubject(asunto);
        message.setText(cuerpo);
        mailSender.send(message);
        log.info("Código de verificación enviado a '{}' por email", destinatario);
    }
}