package com.drogia.modules.usuarios.dto.response;

/** Usuario expuesto al frontend. */
public record UsuarioResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active) {
}
