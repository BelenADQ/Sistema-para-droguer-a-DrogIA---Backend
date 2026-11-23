package com.drogia.util;

import com.drogia.entities.enums.Rol;

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
}
