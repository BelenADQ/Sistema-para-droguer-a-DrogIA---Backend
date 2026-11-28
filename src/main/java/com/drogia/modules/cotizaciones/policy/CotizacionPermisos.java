package com.drogia.modules.cotizaciones.policy;

import com.drogia.shared.enums.Rol;
import java.util.List;

/** Política de roles del módulo de cotizaciones. */
public final class CotizacionPermisos {

    private static final List<Rol> LECTURA =
            List.of(Rol.ADMINISTRACION, Rol.VENTAS);
    private static final List<Rol> ESCRITURA = List.of(Rol.VENTAS);

    private CotizacionPermisos() {
    }

    public static String[] lecturaParaSecurity() {
        return LECTURA.stream().map(Rol::name).toArray(String[]::new);
    }

    public static String[] escrituraParaSecurity() {
        return ESCRITURA.stream().map(Rol::name).toArray(String[]::new);
    }
}
