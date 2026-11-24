package com.drogia.modules.correos.service.gemini;

import com.drogia.modules.correos.entity.enums.TipoCorreo;

public record GeminiClassification(
        TipoCorreo tipoCorreo,
        String codigoCotizacionDetectado,
        String resumenProductos,
        String rawJson) {
}
