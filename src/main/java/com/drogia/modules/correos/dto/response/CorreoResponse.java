package com.drogia.modules.correos.dto.response;

import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Correo expuesto al frontend, con datos estructurados extraídos por la IA. */
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
    private String tipoRespuesta;
    private String codigoCotizacionDetectado;
    private String codigoReferencia;
    private String resumenProductos;
    private Long casoId;
    private String cuerpoTexto;
    private String errorDetalle;
    private JsonNode cliente;
    private JsonNode productos;
    private JsonNode destino;
}
