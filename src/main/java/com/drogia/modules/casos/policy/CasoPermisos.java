package com.drogia.modules.casos.policy;

import com.drogia.modules.correos.policy.CorreoPermisos;
import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles y áreas del módulo de casos. */
public final class CasoPermisos {

    private static final List<Rol> ROLES_PERMITIDOS =
            List.of(Rol.ADMINISTRACION, Rol.VENTAS, Rol.COMPRAS);

    private CasoPermisos() {
    }

    /** Devuelve los roles autorizados para consultar casos. */
    public static String[] rolesParaSecurity() {
        return ROLES_PERMITIDOS.stream().map(Rol::name).toArray(String[]::new);
    }

    /** Devuelve el área permitida según el rol del usuario. */
    public static String areaPorRol(Rol rol) {
        return CorreoPermisos.areaPorRol(rol);
    }
}
