package com.drogia.service.gemini;

import com.drogia.entities.enums.TipoCorreo;

public record GeminiClassification(
        TipoCorreo tipoCorreo,
        String codigoCotizacionDetectado,
        String resumenProductos,
        String rawJson) {
}
