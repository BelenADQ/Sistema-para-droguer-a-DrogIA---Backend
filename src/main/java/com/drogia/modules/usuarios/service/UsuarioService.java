package com.drogia.modules.usuarios.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.usuarios.dto.request.UsuarioRequest;
import com.drogia.modules.usuarios.dto.response.UsuarioResponse;
import com.drogia.modules.usuarios.entity.User;
import com.drogia.modules.usuarios.repository.UserRepository;
import com.drogia.shared.enums.Rol;
import com.drogia.shared.util.RolLabel;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de gestión de usuarios. */
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Lista los usuarios ordenados por identificador. */
    public List<UsuarioResponse> listar() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Devuelve un usuario por su identificador. */
    public UsuarioResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Crea un usuario con el correo como nombre de acceso. */
    public UsuarioResponse crear(UsuarioRequest request) {
        String email = validarEmail(request.email(), null);
        String username = validarUsername(email, null);
        Rol rol = validarRol(request.role());
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .nombre(request.name().trim())
                .rol(rol)
                .enabled(request.active() == null || request.active())
                .build();
        return toResponse(userRepository.save(user));
    }

    /** Actualiza los datos de un usuario. */
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        User user = buscar(id);
        String email = validarEmail(request.email(), id);
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(user.getUsername())) {
            user.setUsername(validarUsername(email, id));
        }
        user.setEmail(email);
        user.setNombre(request.name().trim());
        user.setRol(validarRol(request.role()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.active() != null) {
            user.setEnabled(request.active());
        }
        return toResponse(userRepository.save(user));
    }

    /** Activa o desactiva un usuario. */
    public UsuarioResponse cambiarEstado(Long id, boolean active) {
        User user = buscar(id);
        user.setEnabled(active);
        return toResponse(userRepository.save(user));
    }

    private User buscar(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private String validarEmail(String value, Long id) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El correo es obligatorio");
        }
        String email = value.trim().toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El correo no es válido");
        }
        userRepository.findByEmail(email)
                .filter(otro -> id == null || !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
                });
        return email;
    }

    private String validarUsername(String email, Long id) {
        userRepository.findByUsername(email)
                .filter(otro -> id == null || !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ApiException(HttpStatus.CONFLICT, "El usuario ya está registrado");
                });
        return email;
    }

    private Rol validarRol(String value) {
        Rol rol;
        try {
            rol = RolLabel.toRol(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        if (rol == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }
        return rol;
    }

    private UsuarioResponse toResponse(User user) {
        return new UsuarioResponse(
                user.getId(),
                user.getNombre(),
                user.getEmail(),
                RolLabel.from(user.getRol()),
                user.isEnabled());
    }
}
