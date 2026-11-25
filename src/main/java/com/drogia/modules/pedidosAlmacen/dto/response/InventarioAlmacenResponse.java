package com.drogia.modules.pedidosAlmacen.dto.response;

import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

/** Mercadería recibida y aún no despachada, calculada por el backend. */
public record InventarioAlmacenResponse(
        Long pedidoId,
        Long ordenCompraId,
        String ordenCompraNumero,
        String ventaNumero,
        JsonNode cliente,
        JsonNode destino,
        String tipoDespacho,
        JsonNode productos,
        int cantidadLotes,
        int unidades,
        LocalDate fechaLimiteEntrega,
        long diasRestantes,
        String estado,
        String prioridad,
        String motivo) {
}
