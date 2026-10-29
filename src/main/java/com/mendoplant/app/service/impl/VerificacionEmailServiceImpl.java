package com.mendoplant.app.service.impl;

import com.mendoplant.app.model.VerificacionEmail;
import com.mendoplant.app.repository.VerificacionEmailRepository;
import com.mendoplant.app.service.VerificacionEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificacionEmailServiceImpl implements VerificacionEmailService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificacionEmailRepository repository;
    private final EmailSenderService emailSenderService;

    @Override
    @Transactional
    public String generarYEnviarCodigo(String email) {
        String emailNorm = email.trim().toLowerCase();
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));

        VerificacionEmail registro = VerificacionEmail.builder()
                .email(emailNorm)
                .codigo(codigo)
                .expiraEn(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA))
                .build();
        repository.save(registro);

        emailSenderService.enviarCodigoVerificacion(emailNorm, codigo);
        return codigo;
    }

    @Override
    @Transactional
    public boolean verificar(String email, String codigo) {
        String emailNorm = email.trim().toLowerCase();
        VerificacionEmail registro = repository
                .findFirstByEmailAndUsadaFalseOrderByCreatedAtDesc(emailNorm)
                .orElse(null);

        if (registro == null || !registro.getCodigo().equals(codigo)) {
            return false;
        }
        if (registro.getExpiraEn().isBefore(LocalDateTime.now())) {
            return false;
        }
        registro.setUsada(true);
        repository.save(registro);
        log.info("Email '{}' verificado con el código más reciente", emailNorm);
        return true;
    }
}