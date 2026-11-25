package com.drogia.modules.clientes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de la petición de cliente. */
public record ClienteRequest(
        @NotBlank(message = "el nombre es obligatorio") String name,
        @NotBlank(message = "el documento es obligatorio")
        @Pattern(regexp = "\\d{11}", message = "el documento debe tener 11 dígitos") String document,
        String email) {
}
