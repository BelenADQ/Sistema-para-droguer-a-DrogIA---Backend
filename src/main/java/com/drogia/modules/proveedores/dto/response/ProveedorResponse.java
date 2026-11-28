package com.drogia.modules.proveedores.dto.response;

/** Proveedor expuesto al frontend. */
public record ProveedorResponse(
        Long id,
        String name,
        String document,
        String phone,
        String email) {
}
