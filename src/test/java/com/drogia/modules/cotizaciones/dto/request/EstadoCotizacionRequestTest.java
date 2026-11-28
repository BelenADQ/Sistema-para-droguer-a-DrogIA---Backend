package com.drogia.modules.cotizaciones.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.drogia.modules.cotizaciones.entity.enums.EstadoCotizacion;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Verifica que el cuerpo JSON del cambio de estado sea compatible con el frontend. */
class EstadoCotizacionRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializaEstadoDesdeObjeto() {
        EstadoCotizacionRequest request = mapper.readValue(
                "{\"estado\":\"Aceptada\"}", EstadoCotizacionRequest.class);
        assertEquals(EstadoCotizacion.ACEPTADA, request.estado());
    }

    @Test
    void deserializaEnvioConAliasDelFrontend() {
        EnvioCotizacionRequest request = mapper.readValue(
                "{\"destinatario\":\"cliente@example.com\",\"asunto\":\"Cotización\",\"cuerpo\":\"Hola\"}",
                EnvioCotizacionRequest.class);
        assertEquals("cliente@example.com", request.to());
        assertEquals("Cotización", request.subject());
        assertEquals("Hola", request.body());
    }
}
