package com.drogia.modules.solicitudesCompra.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Estados de una solicitud de compra generada desde una venta. */
public enum EstadoSolicitudCompra {
    PENDIENTE("Pendiente de compra"),
    EN_COMPRA("En compra"),
    RECIBIDO("Recibido");

    private final String label;

    EstadoSolicitudCompra(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoSolicitudCompra from(String value) {
        if (value == null || value.isBlank()) {
            return PENDIENTE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace(" ", "_");
        for (EstadoSolicitudCompra estado : values()) {
            if (estado.name().equals(normalized)
                    || estado.label.toUpperCase(Locale.ROOT).replace(" ", "_").equals(normalized)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de solicitud no soportado: " + value);
    }
}
