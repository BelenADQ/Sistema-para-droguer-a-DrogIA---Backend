package com.drogia.service.gemini;

import com.drogia.entities.enums.TipoCorreo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GeminiClassificationService {

    private static final Set<String> GEMINI_MIME = Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");
    private static final Pattern COT_PATTERN = Pattern.compile("COT-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;

    public GeminiClassificationService(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.5-flash}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.timeout-seconds:60}") long timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    public GeminiClassification classify(String asunto, String cuerpo, String remitente, List<Attachment> attachments) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(asunto, cuerpo, "Gemini no configurado");
        }
        try {
            Map<String, Object> payload = buildPayload(asunto, cuerpo, remitente, attachments);
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/models/" + model + ":generateContent"))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Gemini respondió status={}", response.statusCode());
                return fallback(asunto, cuerpo, "Gemini status " + response.statusCode());
            }
            return parseClassification(extractText(response.body()));
        } catch (Exception e) {
            log.warn("Gemini clasificación falló: {}", e.getClass().getSimpleName());
            return fallback(asunto, cuerpo, e.getClass().getSimpleName());
        }
    }

    private Map<String, Object> buildPayload(String asunto, String cuerpo, String remitente, List<Attachment> attachments) throws Exception {
        List<Map<String, Object>> parts = new ArrayList<>();
        String prompt = "Clasifica el correo corporativo y responde SOLO JSON válido con este esquema exacto: " +
                "{\"tipo_correo\":\"SOLICITUD_COTIZACION|ORDEN_COMPRA|OTRO\",\"codigo_cotizacion_detectado\":\"COT-2026-0045 o null\",\"resumen_productos\":\"...\"}. " +
                "Si es ORDEN_COMPRA detecta el código COT-YYYY-NNNN en asunto/cuerpo/adjuntos visibles. Si no existe usa null. " +
                "Si es spam, boletín o ambiguo usa OTRO. No incluyas markdown ni texto adicional.\n\n" +
                "METADATOS\nRemitente: " + safe(remitente) + "\nAsunto: " + safe(asunto) + "\nCuerpo:\n" + safe(cuerpo);
        parts.add(Map.of("text", prompt));
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                if (attachment.path() != null && attachment.mimeType() != null && GEMINI_MIME.contains(attachment.mimeType().toLowerCase())) {
                    byte[] bytes = Files.readAllBytes(attachment.path());
                    if (bytes.length > 0 && bytes.length <= 6_000_000) {
                        parts.add(Map.of("inline_data", Map.of(
                                "mime_type", attachment.mimeType().toLowerCase(),
                                "data", Base64.getEncoder().encodeToString(bytes))));
                    }
                } else if (attachment.filename() != null) {
                    parts.add(Map.of("text", "Adjunto no analizado: " + attachment.filename() + " (" + attachment.mimeType() + ")"));
                }
            }
        }
        return Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0));
    }

    private String extractText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    return part.get("text").asText();
                }
            }
        }
        throw new IllegalArgumentException("Respuesta Gemini sin texto");
    }

    private GeminiClassification parseClassification(String raw) throws Exception {
        String json = raw == null ? "" : raw.trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        JsonNode node = objectMapper.readTree(json);
        String tipo = node.path("tipo_correo").asText("OTRO").trim().toUpperCase();
        TipoCorreo tipoCorreo = switch (tipo) {
            case "SOLICITUD_COTIZACION" -> TipoCorreo.SOLICITUD_COTIZACION;
            case "ORDEN_COMPRA" -> TipoCorreo.ORDEN_COMPRA;
            default -> TipoCorreo.OTRO;
        };
        String codigo = node.path("codigo_cotizacion_detectado").isNull() ? null : node.path("codigo_cotizacion_detectado").asText(null);
        if (codigo != null) {
            codigo = codigo.trim();
            if (codigo.isBlank() || "null".equalsIgnoreCase(codigo)) {
                codigo = null;
            }
            if (codigo != null) {
                var matcher = COT_PATTERN.matcher(codigo);
                codigo = matcher.find() ? matcher.group().toUpperCase() : null;
            }
        }
        String resumen = node.path("resumen_productos").asText("");
        if (resumen != null && resumen.length() > 2000) {
            resumen = resumen.substring(0, 2000);
        }
        return new GeminiClassification(tipoCorreo, codigo, resumen, json);
    }

    private GeminiClassification fallback(String asunto, String cuerpo, String reason) {
        String text = (safe(asunto) + "\n" + safe(cuerpo)).toUpperCase();
        var matcher = COT_PATTERN.matcher(text);
        String codigo = matcher.find() ? matcher.group().toUpperCase() : null;
        TipoCorreo tipo = codigo != null && text.contains("ORDEN") ? TipoCorreo.ORDEN_COMPRA : TipoCorreo.OTRO;
        return new GeminiClassification(tipo, codigo, "Clasificación manual requerida: " + reason, null);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record Attachment(String filename, String mimeType, Path path) {
    }
}
