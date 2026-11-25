package com.drogia.modules.casos.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Estados del caso generado a partir de un correo clasificado. */
public enum EstadoCaso {
    PENDIENTE,
    ASIGNADO,
    EN_ANALISIS,
    COTIZADO,
    ACEPTADO,
    CONVERTIDO,
    CERRADO;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoCaso from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace(" ", "_");
        return switch (normalized) {
            case "PENDIENTE" -> PENDIENTE;
            case "ASIGNADO" -> ASIGNADO;
            case "EN_ANALISIS" -> EN_ANALISIS;
            case "COTIZADO" -> COTIZADO;
            case "ACEPTADO" -> ACEPTADO;
            case "CONVERTIDO" -> CONVERTIDO;
            case "CERRADO" -> CERRADO;
            default -> throw new IllegalArgumentException("Estado de caso no soportado: " + value);
        };
    }

    public String label() {
        return switch (this) {
            case PENDIENTE -> "Pendiente";
            case ASIGNADO -> "Asignado";
            case EN_ANALISIS -> "En análisis";
            case COTIZADO -> "Cotizado";
            case ACEPTADO -> "Aceptado";
            case CONVERTIDO -> "Convertido";
            case CERRADO -> "Cerrado";
        };
    }
}
