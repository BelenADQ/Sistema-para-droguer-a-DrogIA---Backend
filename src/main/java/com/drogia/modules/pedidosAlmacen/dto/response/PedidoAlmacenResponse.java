package com.drogia.modules.pedidosAlmacen.dto.response;

import tools.jackson.databind.JsonNode;

/** Pedido en almacén expuesto al frontend. */
public record PedidoAlmacenResponse(
        Long id,
        Long saleId,
        String saleNumber,
        Long ordenCompraId,
        Long casoId,
        JsonNode client,
        JsonNode products,
        JsonNode destination,
        String tipoDespacho,
        String status,
        Long assignedToId,
        String assignedTo,
        String fechaAsignacion,
        boolean verificadoInfo,
        boolean verificadoLotes,
        Long despachoId) {
}
