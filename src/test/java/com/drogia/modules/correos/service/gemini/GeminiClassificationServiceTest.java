package com.drogia.modules.correos.service.gemini;

import com.drogia.modules.correos.entity.enums.TipoCorreo;
import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiClassificationServiceTest {

    @Test
    void clasificaRespuestaValida() throws Exception {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/models/gemini-2.5-flash:generateContent", exchange -> {
            capturedKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = """
                    {"candidates":[{"content":{"parts":[{"text":"{\\"tipo_correo\\":\\"ORDEN_COMPRA\\",\\"codigo_cotizacion_detectado\\":\\"COT-2026-0045\\",\\"resumen_productos\\":\\"Medicamentos\\"}"}]}}]}
                    """;
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            GeminiClassificationService service = new GeminiClassificationService(
                    new ObjectMapper(), "test-key", "gemini-2.5-flash", baseUrl, 5);

            GeminiClassification result = service.classify(
                    "Orden de compra", "Adjunto COT-2026-0045", "compras@test.com", List.of());

            assertEquals(TipoCorreo.ORDEN_COMPRA, result.tipoCorreo());
            assertEquals("COT-2026-0045", result.codigoCotizacionDetectado());
            assertEquals("Medicamentos", result.resumenProductos());
            assertEquals("test-key", capturedKey.get());
            assertTrue(capturedBody.get().contains("ORDEN_COMPRA"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void aplicaFallbackSinApiKey() {
        GeminiClassificationService service = new GeminiClassificationService(
                new ObjectMapper(), "", "gemini-2.5-flash", "http://localhost:1", 5);

        GeminiClassification result = service.classify(
                "Orden de compra", "Referencia COT-2026-0099", "compras@test.com", List.of());

        assertEquals(TipoCorreo.ORDEN_COMPRA, result.tipoCorreo());
        assertEquals("COT-2026-0099", result.codigoCotizacionDetectado());
        assertNull(result.rawJson());
    }

    @Test
    void siguienteTextoSeEnviaComoAdjuntoNoAnalizado() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/models/gemini-2.5-flash:generateContent", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = """
                    {"candidates":[{"content":{"parts":[{"text":"{\\"tipo_correo\\":\\"OTRO\\",\\"codigo_cotizacion_detectado\\":null,\\"resumen_productos\\":\\"\\"}"}]}}]}
                    """;
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            GeminiClassificationService service = new GeminiClassificationService(
                    new ObjectMapper(), "test-key", "gemini-2.5-flash", baseUrl, 5);

            GeminiClassification result = service.classify("Consulta", "Texto", "user@test.com",
                    List.of(new GeminiClassificationService.Attachment("listado.xlsx",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", null)));

            assertEquals(TipoCorreo.OTRO, result.tipoCorreo());
            assertNull(result.codigoCotizacionDetectado());
            assertTrue(capturedBody.get().contains("Adjunto no analizado: listado.xlsx"));
        } finally {
            server.stop(0);
        }
    }
}
