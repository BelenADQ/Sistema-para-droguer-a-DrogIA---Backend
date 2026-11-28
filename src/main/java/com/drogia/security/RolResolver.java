package com.drogia.security;

import com.drogia.shared.enums.Rol;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Resuelve el rol del sistema a partir de la autenticación JWT. */
public final class RolResolver {

    private RolResolver() {
    }

    /** Convierte las autoridades del token en el enum Rol. */
    public static Rol from(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        if (authorities.contains("ROLE_ADMINISTRACION")) {
            return Rol.ADMINISTRACION;
        }
        if (authorities.contains("ROLE_VENTAS")) {
            return Rol.VENTAS;
        }
        if (authorities.contains("ROLE_COMPRAS")) {
            return Rol.COMPRAS;
        }
        if (authorities.contains("ROLE_ALMACEN")) {
            return Rol.ALMACEN;
        }
        return null;
    }
}
