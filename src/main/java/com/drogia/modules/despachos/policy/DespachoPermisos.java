package com.drogia.modules.despachos.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles de los despachos. */
public final class DespachoPermisos {

    private static final List<Rol> LECTURA =
            List.of(Rol.ADMINISTRACION, Rol.VENTAS, Rol.COMPRAS, Rol.ALMACEN);
    private static final List<Rol> ESCRITURA = List.of(Rol.ALMACEN);

    private DespachoPermisos() {
    }

    public static String[] lecturaParaSecurity() {
        return LECTURA.stream().map(Rol::name).toArray(String[]::new);
    }

    public static String[] escrituraParaSecurity() {
        return ESCRITURA.stream().map(Rol::name).toArray(String[]::new);
    }
}
