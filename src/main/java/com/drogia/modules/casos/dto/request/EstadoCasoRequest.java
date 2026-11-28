package com.drogia.modules.casos.dto.request;

import com.drogia.modules.casos.entity.enums.EstadoCaso;
import jakarta.validation.constraints.NotNull;

/** Cuerpo para el cambio manual de estado de un caso. */
public record EstadoCasoRequest(
        @NotNull(message = "el estado es obligatorio") EstadoCaso estado,
        String observaciones) {
}
