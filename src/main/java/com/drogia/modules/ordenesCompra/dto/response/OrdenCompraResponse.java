package com.drogia.modules.ordenesCompra.dto.response;

import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

/** Orden de compra expuesta al frontend. */
public record OrdenCompraResponse(
        Long id,
        String number,
        JsonNode supplier,
        String date,
        List<ItemResponse> items,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        String status,
        List<Long> requestIds,
        String saleNumber,
        Long casoId) {

    public record ItemResponse(
            Long productId,
            String product,
            String unit,
            Integer quantity,
            BigDecimal price,
            BigDecimal subtotal) {
    }
}
