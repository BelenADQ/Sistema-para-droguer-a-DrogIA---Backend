package com.drogia.service;

import com.drogia.dto.request.LoginRequest;
import com.drogia.dto.response.LoginResponse;
import com.drogia.entities.User;
import com.drogia.repository.UserRepository;
import com.drogia.security.JwtService;
import com.drogia.util.RolLabel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Servicio que valida credenciales y genera el token de acceso. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (Exception e) {
            log.warn("Login fallido: usuario={}", request.getUsername());
            throw e;
        }

        User user = userRepository.findByUsername(request.getUsername())

                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + request.getUsername()));

        String token = jwtService.generateToken(user);
        log.info("Login exitoso para el:  usuario={} rol={}", user.getUsername(), user.getRol().name());
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .name(user.getNombre())
                .role(RolLabel.from(user.getRol()))
                .rol(user.getRol().name())
                .expiresIn(jwtService.getExpirationSeconds())
                .build();
    }
}
