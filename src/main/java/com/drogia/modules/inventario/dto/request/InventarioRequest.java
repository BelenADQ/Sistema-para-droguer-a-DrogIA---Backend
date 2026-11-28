package com.drogia.modules.inventario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos de un lote en inventario. */
public record InventarioRequest(
        Long productoId,
        @NotBlank(message = "el producto es obligatorio") String product,
        String purchaseOrderCode,
        String lot,
        @NotNull(message = "el stock es obligatorio") Integer stock,
        Integer minimumStock,
        String expiration) {
}
