package com.drogia.modules.cotizaciones.dto.request;

import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Datos necesarios para crear o actualizar una cotización desde un caso. */
public record CotizacionRequest(
        Long casoId,
        Long clienteId,
        String cliente,
        String clienteDocumento,
        String clienteEmail,
        LocalDate fecha,
        String condicionesPago,
        String vigencia,
        List<ItemRequest> items,
        BigDecimal subtotal,
        BigDecimal igv,
        JsonNode destino,
        JsonNode proveedores,
        String observaciones) {

    /** Ítem solicitado dentro de la cotización. */
    public record ItemRequest(
            Long productoId,
            String producto,
            String unidad,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }

}
