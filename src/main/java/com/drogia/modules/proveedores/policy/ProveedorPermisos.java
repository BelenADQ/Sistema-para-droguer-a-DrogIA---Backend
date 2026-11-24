package com.drogia.modules.proveedores.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles del módulo de proveedores. */
public final class ProveedorPermisos {

    private static final List<Rol> ROLES_PERMITIDOS =
            List.of(Rol.ADMINISTRACION, Rol.COMPRAS);

    private ProveedorPermisos() {
    }

    /** Devuelve los roles autorizados para el módulo de proveedores. */
    public static String[] rolesParaSecurity() {
        return ROLES_PERMITIDOS.stream().map(Rol::name).toArray(String[]::new);
    }
}
