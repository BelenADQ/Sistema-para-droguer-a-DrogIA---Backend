package com.drogia.shared.dto;

import jakarta.validation.constraints.NotBlank;

/** Solicitud genérica de cambio de estado. */
public record EstadoRequest(@NotBlank(message = "el estado es obligatorio") String estado) {
}
