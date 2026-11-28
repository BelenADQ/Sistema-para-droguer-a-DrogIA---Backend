package com.drogia.modules.ventas.dto.request;

import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Datos para registrar una venta directa. */
public record VentaRequest(
        Long cotizacionId,
        Long casoId,
        JsonNode client,
        LocalDate date,
        List<ItemRequest> items,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total) {

    public record ItemRequest(
            Long id,
            String name,
            String unit,
            Integer quantity,
            BigDecimal price,
            BigDecimal subtotal) {
    }
}
