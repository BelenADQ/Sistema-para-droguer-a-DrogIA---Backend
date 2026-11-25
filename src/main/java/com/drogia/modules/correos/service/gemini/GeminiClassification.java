package com.drogia.modules.correos.service.gemini;

import com.drogia.modules.correos.entity.enums.TipoCorreo;
import com.drogia.modules.correos.entity.enums.TipoRespuesta;

/** Resultado estructurado del análisis de un correo. */
public record GeminiClassification(
        TipoCorreo tipoCorreo,
        TipoRespuesta tipoRespuesta,
        String codigoCotizacionDetectado,
        String codigoReferencia,
        String clienteJson,
        String productosJson,
        String destinoJson,
        String resumenProductos,
        String rawJson) {
}
