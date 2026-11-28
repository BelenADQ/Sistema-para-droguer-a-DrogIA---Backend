package com.drogia.modules.inventario.dto.response;

/** Lote expuesto al frontend con el contrato de InventarioPage. */
public record InventarioResponse(
        Long id,
        Long productId,
        String product,
        String purchaseOrderCode,
        String lot,
        Integer stock,
        Integer minimumStock,
        String expiration) {
}
