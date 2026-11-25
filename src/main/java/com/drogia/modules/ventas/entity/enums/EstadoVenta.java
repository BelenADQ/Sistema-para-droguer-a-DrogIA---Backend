package com.drogia.modules.ventas.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Estados de una venta derivada de una cotización. */
public enum EstadoVenta {
    PENDIENTE_COMPRA("Pendiente de compra"),
    EN_PROCESO("En proceso"),
    DISPONIBLE("Disponible"),
    EN_DESPACHO("En despacho"),
    EN_TRANSITO("En tránsito"),
    ENTREGADA("Entregada"),
    CANCELADA("Cancelada");

    private final String label;

    EstadoVenta(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoVenta from(String value) {
        if (value == null || value.isBlank()) {
            return PENDIENTE_COMPRA;
        }
        String normalized = normalizar(value);
        for (EstadoVenta estado : values()) {
            if (normalizado(estado.name()).equals(normalized)
                    || normalizado(estado.label).equals(normalized)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de venta no soportado: " + value);
    }

    private static String normalizar(String value) {
        return normalizado(value).replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace("Á", "A");
    }

    private static String normalizado(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
    }
}
