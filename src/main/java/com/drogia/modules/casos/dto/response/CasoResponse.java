package com.drogia.modules.casos.dto.response;

import tools.jackson.databind.JsonNode;
import java.time.LocalDateTime;

/** Caso expuesto al frontend, con alias para el contrato actual. */
public record CasoResponse(
        Long id,
        Long correoId,
        String remitente,
        String asunto,
        String area,
        String estado,
        String status,
        Long asignadoAId,
        String asignadoANombre,
        LocalDateTime fechaAsignacion,
        Long cotizacionId,
        String codigoSolicitud,
        LocalDateTime fechaLimiteCotizacion,
        JsonNode cliente,
        JsonNode productos,
        JsonNode destino,
        Long ventaId,
        Long ordenCompraId,
        Long pedidoAlmacenId,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCierre,
        String observaciones,
        String codigoCotizacionDetectado,
        String resumenProductos) {
}
