package com.drogia.modules.clientes.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles del módulo de clientes. */
public final class ClientePermisos {

    private static final List<Rol> ROLES_PERMITIDOS =
            List.of(Rol.ADMINISTRACION, Rol.VENTAS);

    private ClientePermisos() {
    }

    /** Devuelve los roles autorizados para el módulo de clientes. */
    public static String[] rolesParaSecurity() {
        return ROLES_PERMITIDOS.stream().map(Rol::name).toArray(String[]::new);
    }
}
