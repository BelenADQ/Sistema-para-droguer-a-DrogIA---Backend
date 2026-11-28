package com.drogia.shared.util;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.CollectionType;

import java.util.List;

/** Utilidad pequeña para persistir y leer fragmentos JSON en entidades. */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("No se pudo serializar la información", ex);
        }
    }

    public static JsonNode read(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(value);
        } catch (JacksonException ex) {
            return null;
        }
    }

    public static <T> List<T> readList(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            CollectionType collectionType = MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, type);
            List<T> resultado = MAPPER.readValue(value, collectionType);
            return resultado == null ? List.of() : resultado;
        } catch (JacksonException ex) {
            return List.of();
        }
    }
}
