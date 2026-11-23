package com.drogia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorreoResponse {
    private Long id;
    private String messageId;
    private String sender;
    private String subject;
    private String date;
    private String area;
    private String status;
    private String tipoClasificacion;
    private String codigoCotizacionDetectado;
    private String resumenProductos;
}
