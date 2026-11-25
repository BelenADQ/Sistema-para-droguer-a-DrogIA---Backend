package com.drogia.modules.cotizaciones.dto.request;

import com.drogia.modules.cotizaciones.entity.enums.EstadoCotizacion;

/** Estado solicitado para una cotización, con nombres del backend y del frontend. */
public record EstadoCotizacionRequest(EstadoCotizacion estado) {
}
