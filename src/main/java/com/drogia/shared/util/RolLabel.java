package com.drogia.shared.util;

import com.drogia.shared.enums.Rol;

public final class RolLabel {

    private RolLabel() {
    }

    public static String from(Rol rol) {
        if (rol == null) {
            return null;
        }
        return switch (rol) {
            case ADMINISTRACION -> "Administrador";
            case VENTAS -> "Vendedor";
            case COMPRAS -> "Comprador";
            case ALMACEN -> "Almacenero";
        };
    }

    /** Convierte la etiqueta del frontend o el nombre del enum en Rol. */
    public static Rol toRol(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim()) {
            case "Administrador", "ADMINISTRACION" -> Rol.ADMINISTRACION;
            case "Vendedor", "VENTAS" -> Rol.VENTAS;
            case "Comprador", "COMPRAS" -> Rol.COMPRAS;
            case "Almacenero", "ALMACEN" -> Rol.ALMACEN;
            default -> throw new IllegalArgumentException("Rol no soportado: " + value);
        };
    }
}
