package com.mendoplant.app.service;

import com.mendoplant.app.dto.ChangePasswordDTO;
import com.mendoplant.app.dto.UserRegistrationDTO;
import com.mendoplant.app.model.Role;
import com.mendoplant.app.model.User;

import java.util.List;

public interface UserService {
    User registrar(UserRegistrationDTO dto);
    /** Lanza BusinessException si el email ya está registrado. Usado como primer paso del registro. */
    void validarEmailDisponible(String email);
    User buscarPorEmail(String email);
    User buscarPorId(Long id);
    List<User> listarTodos();
    void cambiarPassword(Long userId, ChangePasswordDTO dto);
    void cambiarNombre(Long userId, String nombre);

    /** Guarda la foto de perfil del usuario (validando que sea una imagen) y devuelve su URL. */
    String cambiarAvatar(Long userId, org.springframework.web.multipart.MultipartFile archivo);
    void cambiarRol(Long userId, Role nuevoRol);
    void cambiarEstado(Long userId, boolean activo);
}
