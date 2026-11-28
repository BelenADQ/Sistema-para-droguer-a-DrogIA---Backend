package com.drogia.modules.correos.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles y áreas del módulo de correos. */
public final class CorreoPermisos {

    public static final String SIN_ROL = "__SIN_ROL__";

    private static final List<Rol> ROLES_PERMITIDOS =
            List.of(Rol.ADMINISTRACION, Rol.VENTAS, Rol.COMPRAS);

    private CorreoPermisos() {
    }

    /** Devuelve los roles autorizados para el módulo de correos. */
    public static String[] rolesParaSecurity() {
        return ROLES_PERMITIDOS.stream().map(Rol::name).toArray(String[]::new);
    }

    /** Devuelve el área permitida según el rol del usuario. */
    public static String areaPorRol(Rol rol) {
        if (rol == null) {
            return SIN_ROL;
        }
        return switch (rol) {
            case ADMINISTRACION -> null;
            case VENTAS -> "Ventas";
            case COMPRAS -> "Compras";
            default -> SIN_ROL;
        };
    }
}
