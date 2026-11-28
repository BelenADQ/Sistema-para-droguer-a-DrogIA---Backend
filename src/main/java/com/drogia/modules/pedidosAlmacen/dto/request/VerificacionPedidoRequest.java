package com.drogia.modules.pedidosAlmacen.dto.request;

/** Resultado de la verificación realizada por el almacenero. */
public record VerificacionPedidoRequest(boolean verificadoInfo, boolean verificadoLotes) {
}
