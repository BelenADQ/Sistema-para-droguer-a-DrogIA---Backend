package com.drogia.modules.cotizaciones.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/** Estados del seguimiento de una cotización. */
public enum EstadoCotizacion {
    PENDIENTE("Pendiente"),
    ENVIADA("Enviada"),
    ACEPTADA("Aceptada"),
    CONVERTIDA("Convertida");

    private final String label;

    EstadoCotizacion(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Acepta el valor interno y la etiqueta mostrada en el frontend. */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoCotizacion from(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El estado de la cotización es obligatorio");
        }
        for (EstadoCotizacion estado : values()) {
            if (estado.name().equalsIgnoreCase(valor.trim())
                    || estado.label.equalsIgnoreCase(valor.trim())) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de cotización no válido: " + valor);
    }
}
