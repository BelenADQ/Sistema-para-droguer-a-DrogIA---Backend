package com.drogia.modules.correos.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/** Estados posibles de un correo. */
public enum EstadoCorreo {
    PENDIENTE,
    ANALIZADO,
    REVISION;

    @JsonCreator
    public static EstadoCorreo from(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");
        return switch (normalized) {
            case "PENDIENTE" -> PENDIENTE;
            case "ANALIZADO" -> ANALIZADO;
            case "REVISION", "REVISION MANUAL" -> REVISION;
            default -> throw new IllegalArgumentException("Estado no soportado: " + value);
        };
    }

    public String label() {
        return switch (this) {
            case PENDIENTE -> "Pendiente";
            case ANALIZADO -> "Analizado";
            case REVISION -> "Revisión manual";
        };
    }
}
