package com.drogia.modules.cotizaciones.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/** Solicitud de envío de una cotización por correo con PDF adjunto. */
public record EnvioCotizacionRequest(
        @JsonAlias("destinatario")
        @NotBlank(message = "el destinatario es obligatorio") String to,
        @JsonAlias("asunto") String subject,
        @JsonAlias("cuerpo") String body,
        String pdfFileName,
        String pdfBase64,
        JsonNode quote) {
}
