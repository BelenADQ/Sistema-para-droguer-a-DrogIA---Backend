package com.drogia.modules.cotizaciones.dto.response;

import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Cotización expuesta al frontend con contrato compatible con CotizacionesPage. */
public record CotizacionResponse(
        Long id,
        String numero,
        String number,
        Long casoId,
        Long correoId,
        Long creadoPorId,
        String creadoPorNombre,
        String asignadoANombre,
        String cliente,
        ClienteResponse client,
        String estado,
        String status,
        LocalDateTime fechaCreacion,
        String date,
        String condicionesPago,
        String vigencia,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        String observaciones,
        List<ItemResponse> items,
        List<ProductoResponse> products,
        JsonNode destino,
        JsonNode proveedores,
        String codigoSolicitud,
        LocalDateTime fechaLimiteCotizacion,
        LocalDateTime fechaEnvio,
        String correoAsunto,
        String correoRemitente,
        String area) {

    /** Cliente expuesto en el objeto client del frontend. */
    public record ClienteResponse(Long id, String name, String document, String email) {
    }

    /** Ítem en el formato usado por el PDF y la tabla del frontend. */
    public record ProductoResponse(
            Long id,
            String name,
            String unit,
            Integer quantity,
            BigDecimal price,
            BigDecimal subtotal,
            Integer item) {
    }

    /** Ítem expuesto con nombres del backend. */
    public record ItemResponse(
            Long productoId,
            String producto,
            String unidad,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }
}
