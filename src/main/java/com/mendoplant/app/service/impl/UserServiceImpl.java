package com.mendoplant.app.service.impl;

import com.mendoplant.app.dto.ChangePasswordDTO;
import com.mendoplant.app.dto.UserRegistrationDTO;
import com.mendoplant.app.exception.BusinessException;
import com.mendoplant.app.exception.ResourceNotFoundException;
import com.mendoplant.app.model.Role;
import com.mendoplant.app.model.User;
import com.mendoplant.app.repository.UserRepository;
import com.mendoplant.app.service.ImageStorageService;
import com.mendoplant.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional
    public User registrar(UserRegistrationDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Las contraseñas no coinciden");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Ya existe una cuenta registrada con ese email");
        }
        User user = User.builder()
                .nombre(dto.getNombre())
                .email(dto.getEmail().toLowerCase())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.ROLE_USER)
                .estado(true)
                .build();
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public void validarEmailDisponible(String email) {
        if (userRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new BusinessException("Ya existe una cuenta registrada con ese email");
        }
    }

    @Override
    public User buscarPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    @Override
    public User buscarPorId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    @Override
    public List<User> listarTodos() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void cambiarPassword(Long userId, ChangePasswordDTO dto) {
        User user = buscarPorId(userId);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("La contraseña actual no es correcta");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new BusinessException("La nueva contraseña y su confirmación no coinciden");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void cambiarNombre(Long userId, String nombre) {
        User user = buscarPorId(userId);
        if (nombre == null || nombre.isBlank() || nombre.trim().length() < 2) {
            throw new BusinessException("El nombre debe tener al menos 2 caracteres");
        }
        user.setNombre(nombre.trim());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public String cambiarAvatar(Long userId, org.springframework.web.multipart.MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Debés elegir un archivo de imagen");
        }
        String url = imageStorageService.guardarAvatar(archivo);
        User user = buscarPorId(userId);
        String anterior = user.getAvatarUrl();
        user.setAvatarUrl(url);
        userRepository.save(user);
        if (anterior != null && !anterior.isBlank()) {
            imageStorageService.eliminar(anterior);
        }
        return url;
    }

    @Override
    @Transactional
    public void cambiarRol(Long userId, Role nuevoRol) {
        User user = buscarPorId(userId);
        user.setRole(nuevoRol);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long userId, boolean activo) {
        User user = buscarPorId(userId);
        user.setEstado(activo);
        userRepository.save(user);
    }
}
