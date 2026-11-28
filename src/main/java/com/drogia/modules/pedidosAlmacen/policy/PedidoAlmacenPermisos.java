package com.drogia.modules.pedidosAlmacen.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles de los pedidos en almacén. */
public final class PedidoAlmacenPermisos {

    private static final List<Rol> LECTURA =
            List.of(Rol.ADMINISTRACION, Rol.VENTAS, Rol.COMPRAS, Rol.ALMACEN);
    private static final List<Rol> ESCRITURA = List.of(Rol.ALMACEN);

    private PedidoAlmacenPermisos() {
    }

    public static String[] lecturaParaSecurity() {
        return LECTURA.stream().map(Rol::name).toArray(String[]::new);
    }

    public static String[] escrituraParaSecurity() {
        return ESCRITURA.stream().map(Rol::name).toArray(String[]::new);
    }
}
