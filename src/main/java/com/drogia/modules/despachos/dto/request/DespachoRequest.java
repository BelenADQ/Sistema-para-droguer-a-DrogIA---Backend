package com.drogia.modules.despachos.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos del despacho registrado por Almacenero. */
public record DespachoRequest(
        Long pedidoId,
        String thirdPartyName,
        String thirdPartyDoc,
        String product,
        String purchaseOrderCode,
        Integer quantity,
        @NotNull(message = "la fecha es obligatoria") LocalDate date,
        String courierEmpresa,
        String courierServicio,
        String numeroGuia,
        BigDecimal costoEnvio) {
}
