package com.drogia.modules.ordenesCompra.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Estados de una orden de compra hasta la recepción en almacén. */
public enum EstadoOrdenCompra {
    PENDIENTE("Pendiente"),
    EN_COMPRA("En compra"),
    EN_TRANSITO("En tránsito"),
    RECIBIDO("Recibido"),
    CERRADO("Cerrado");

    private final String label;

    EstadoOrdenCompra(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoOrdenCompra from(String value) {
        if (value == null || value.isBlank()) {
            return PENDIENTE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace(" ", "_");
        for (EstadoOrdenCompra estado : values()) {
            if (estado.name().equals(normalized)
                    || estado.label.toUpperCase(Locale.ROOT).replace(" ", "_").equals(normalized)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de orden no soportado: " + value);
    }
}
