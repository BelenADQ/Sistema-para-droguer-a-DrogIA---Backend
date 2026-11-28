package com.drogia.modules.solicitudesCompra.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles de las solicitudes de compra. */
public final class SolicitudCompraPermisos {

    private static final List<Rol> LECTURA =
            List.of(Rol.ADMINISTRACION, Rol.VENTAS, Rol.COMPRAS, Rol.ALMACEN);
    private static final List<Rol> ESCRITURA = List.of(Rol.COMPRAS);

    private SolicitudCompraPermisos() {
    }

    public static String[] lecturaParaSecurity() {
        return LECTURA.stream().map(Rol::name).toArray(String[]::new);
    }

    public static String[] escrituraParaSecurity() {
        return ESCRITURA.stream().map(Rol::name).toArray(String[]::new);
    }
}
