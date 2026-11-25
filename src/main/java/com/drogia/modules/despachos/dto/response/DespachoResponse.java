package com.drogia.modules.despachos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Despacho expuesto al frontend con el contrato de DespachoPage. */
public record DespachoResponse(
        Long id,
        String code,
        Long pedidoId,
        Long saleId,
        String tipoDespacho,
        String thirdPartyName,
        String thirdPartyDoc,
        String product,
        String purchaseOrderCode,
        Integer quantity,
        LocalDate date,
        String status,
        String courierEmpresa,
        String courierServicio,
        String numeroGuia,
        BigDecimal costoEnvio) {
}
