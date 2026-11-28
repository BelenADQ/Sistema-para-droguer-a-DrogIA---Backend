package com.drogia.modules.ventas.dto.response;

import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

/** Venta expuesta al frontend con el contrato que usa VentasPage. */
public record VentaResponse(
        Long id,
        String number,
        String quoteNumber,
        Long cotizacionId,
        Long casoId,
        JsonNode client,
        String date,
        List<ItemResponse> items,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        String status) {

    public record ItemResponse(
            Long id,
            String name,
            String unit,
            Integer quantity,
            BigDecimal price,
            BigDecimal subtotal) {
    }
}
