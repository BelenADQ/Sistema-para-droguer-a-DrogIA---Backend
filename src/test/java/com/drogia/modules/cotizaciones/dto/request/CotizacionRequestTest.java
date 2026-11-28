package com.drogia.modules.cotizaciones.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Verifica que el payload del frontend no falle por campos visuales adicionales. */
class CotizacionRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void ignoraCamposVisualesAdicionales() {
        String json = """
                {
                  "casoId": 1,
                  "cliente": "Cliente QA",
                  "items": [
                    {"producto": "Paracetamol", "cantidad": 2, "precioUnitario": 12.5, "item": 1}
                  ]
                }
                """;
        CotizacionRequest request = mapper.readValue(json, CotizacionRequest.class);
        assertEquals(2, request.items().get(0).cantidad());
        assertEquals(1, request.casoId());
    }
}
