package com.drogia.modules.proveedores.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de la petición de proveedor. */
public record ProveedorRequest(
        @NotBlank(message = "el nombre es obligatorio") String name,
        @NotBlank(message = "el documento es obligatorio")
        @Pattern(regexp = "\\d{11}", message = "el documento debe tener 11 dígitos") String document,
        @NotBlank(message = "el teléfono es obligatorio") String phone,
        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no es válido") String email) {
}
