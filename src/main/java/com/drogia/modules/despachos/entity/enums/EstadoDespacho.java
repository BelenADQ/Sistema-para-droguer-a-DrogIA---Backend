package com.drogia.modules.despachos.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Estados de un despacho local o por courier. */
public enum EstadoDespacho {
    PROGRAMADO("Programado"),
    EN_TRANSITO("En tránsito"),
    ENTREGADO("Entregado");

    private final String label;

    EstadoDespacho(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoDespacho from(String value) {
        if (value == null || value.isBlank()) {
            return PROGRAMADO;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace(" ", "_");
        for (EstadoDespacho estado : values()) {
            if (estado.name().equals(normalized)
                    || estado.label.toUpperCase(Locale.ROOT).replace(" ", "_").equals(normalized)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de despacho no soportado: " + value);
    }
}
