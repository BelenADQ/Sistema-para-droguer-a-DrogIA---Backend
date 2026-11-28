package com.drogia.modules.usuarios.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de la petición de usuario. */
public record UsuarioRequest(
        @NotBlank(message = "el nombre es obligatorio") String name,
        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no es válido") String email,
        @NotBlank(message = "el rol es obligatorio") String role,
        String password,
        Boolean active) {
}
