package com.drogia.modules.productos.dto.response;

import java.math.BigDecimal;

/** Producto expuesto al frontend. */
public record ProductoResponse(
        Long id,
        String name,
        String unit,
        String lot,
        String healthRegistration,
        BigDecimal price,
        Integer stock,
        String expiration) {
}
