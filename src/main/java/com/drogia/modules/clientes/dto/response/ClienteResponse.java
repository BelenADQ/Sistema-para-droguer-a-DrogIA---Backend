package com.drogia.modules.clientes.dto.response;

/** Cliente expuesto al frontend. */
public record ClienteResponse(Long id, String name, String document, String email) {
}
