package com.drogia.modules.productos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Datos de un producto del catálogo. */
public record ProductoRequest(
        @NotBlank(message = "el nombre es obligatorio") String name,
        String unit,
        String lot,
        String healthRegistration,
        @NotNull(message = "el precio es obligatorio") BigDecimal price,
        @NotNull(message = "el stock es obligatorio") Integer stock,
        String expiration) {
}
