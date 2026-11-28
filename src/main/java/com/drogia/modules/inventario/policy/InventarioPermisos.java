package com.drogia.modules.inventario.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles del inventario. */
public final class InventarioPermisos {

    private static final List<Rol> LECTURA =
            List.of(Rol.ADMINISTRACION, Rol.VENTAS, Rol.COMPRAS, Rol.ALMACEN);
    private static final List<Rol> ESCRITURA = List.of(Rol.COMPRAS, Rol.ALMACEN);

    private InventarioPermisos() {
    }

    public static String[] lecturaParaSecurity() {
        return LECTURA.stream().map(Rol::name).toArray(String[]::new);
    }

    public static String[] escrituraParaSecurity() {
        return ESCRITURA.stream().map(Rol::name).toArray(String[]::new);
    }
}
