package com.drogia.modules.ordenesCompra.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Datos de una orden de compra. */
public record OrdenCompraRequest(
        Long proveedorId,
        Long casoId,
        List<Long> solicitudIds,
        LocalDate date,
        List<ItemRequest> items,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total) {

    public record ItemRequest(
            Long productId,
            String product,
            String unit,
            Integer quantity,
            BigDecimal price,
            BigDecimal subtotal) {
    }
}
