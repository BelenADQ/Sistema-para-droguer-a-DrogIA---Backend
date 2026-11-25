package com.drogia.modules.pedidosAlmacen.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Estados del pedido en almacén hasta la entrega al cliente. */
public enum EstadoPedidoAlmacen {
    PENDIENTE("Pendiente"),
    ASIGNADO("Asignado"),
    VERIFICADO("Verificado"),
    EN_DESPACHO("En despacho"),
    EN_TRANSITO("En tránsito"),
    ENTREGADO("Entregado");

    private final String label;

    EstadoPedidoAlmacen(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoPedidoAlmacen from(String value) {
        if (value == null || value.isBlank()) {
            return PENDIENTE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace(" ", "_");
        for (EstadoPedidoAlmacen estado : values()) {
            if (estado.name().equals(normalized)
                    || estado.label.toUpperCase(Locale.ROOT).replace(" ", "_").equals(normalized)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de pedido no soportado: " + value);
    }
}
