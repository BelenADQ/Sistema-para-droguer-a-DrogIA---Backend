package com.drogia.modules.usuarios.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles del módulo de usuarios. */
public final class UsuarioPermisos {

    private static final List<Rol> ROLES_PERMITIDOS = List.of(Rol.ADMINISTRACION);

    private UsuarioPermisos() {
    }

    /** Devuelve los roles autorizados para el módulo de usuarios. */
    public static String[] rolesParaSecurity() {
        return ROLES_PERMITIDOS.stream().map(Rol::name).toArray(String[]::new);
    }
}
