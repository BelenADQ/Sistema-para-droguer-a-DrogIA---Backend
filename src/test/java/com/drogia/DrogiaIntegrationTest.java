package com.drogia;

import com.drogia.modules.correos.entity.Correo;
import com.drogia.modules.correos.entity.enums.EstadoCorreo;
import com.drogia.modules.correos.repository.CorreoRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrogiaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CorreoRepository correoRepository;

    @Test
    void loginClasificacionEHistorialConContratoDelFrontend() throws Exception {
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.name").value("Administrador"))
                .andExpect(jsonPath("$.role").value("Administrador"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode login = objectMapper.readTree(loginBody);
        String token = login.path("token").asText();
        assertFalse(token.isBlank());

        Correo correo = correoRepository.save(Correo.builder()
                .messageId("test-message-001")
                .carpeta("INBOX")
                .remitente("compras@rodriguez.com")
                .asunto("Orden de compra")
                .cuerpoTexto("Se adjunta orden de compra con referencia COT-2026-0045")
                .fecha(LocalDateTime.of(2026, 10, 6, 10, 0))
                .estado(EstadoCorreo.PENDIENTE)
                .fechaGuardado(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/api/correos/{id}/clasificar", correo.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("compras@rodriguez.com"))
                .andExpect(jsonPath("$.subject").value("Orden de compra"))
                .andExpect(jsonPath("$.date").value("06/10/2026"))
                .andExpect(jsonPath("$.status").value("Analizado"))
                .andExpect(jsonPath("$.area").value("Compras"))
                .andExpect(jsonPath("$.tipoClasificacion").value("ORDEN_COMPRA"))
                .andExpect(jsonPath("$.codigoCotizacionDetectado").value("COT-2026-0045"));

        mockMvc.perform(get("/api/correos/historial")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sender").value("compras@rodriguez.com"))
                .andExpect(jsonPath("$[0].status").value("Analizado"));

        Correo actualizado = correoRepository.findById(correo.getId()).orElseThrow();
        assertEquals(EstadoCorreo.ANALIZADO, actualizado.getEstado());
        assertEquals("Compras", actualizado.getArea());
    }
}
