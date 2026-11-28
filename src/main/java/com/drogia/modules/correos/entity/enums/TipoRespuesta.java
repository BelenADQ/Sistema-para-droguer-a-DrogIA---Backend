package com.drogia.modules.correos.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Tipo de respuesta detectada en un correo posterior a una cotización. */
public enum TipoRespuesta {
    NUEVA_SOLICITUD,
    ORDEN_COMPRA,
    OTRO;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoRespuesta from(String value) {
        if (value == null || value.isBlank()) {
            return OTRO;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace(" ", "_");
        return switch (normalized) {
            case "NUEVA_SOLICITUD" -> NUEVA_SOLICITUD;
            case "ORDEN_COMPRA" -> ORDEN_COMPRA;
            default -> OTRO;
        };
    }
}
