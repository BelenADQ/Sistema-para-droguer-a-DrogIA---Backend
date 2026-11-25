package com.drogia.modules.solicitudesCompra.dto.response;

/** Solicitud de compra expuesta al frontend. */
public record SolicitudCompraResponse(
        Long id,
        Long saleId,
        String saleNumber,
        Long productId,
        String product,
        Integer quantity,
        String status,
        Long orderId) {
}
