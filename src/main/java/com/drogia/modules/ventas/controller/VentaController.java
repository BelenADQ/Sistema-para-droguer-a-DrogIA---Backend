package com.drogia.modules.ventas.controller;

import com.drogia.modules.ventas.dto.request.VentaRequest;
import com.drogia.modules.ventas.dto.response.VentaResponse;
import com.drogia.modules.ventas.service.VentaService;
import com.drogia.shared.dto.EstadoRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador de ventas. */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar() {
        return ResponseEntity.ok(ventaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<VentaResponse> crear(
            @Valid @RequestBody VentaRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crear(request, authentication));
    }

    /** Convierte una cotización aceptada en venta y genera solicitudes de compra. */
    @PostMapping("/desde-cotizacion/{cotizacionId}")
    public ResponseEntity<VentaResponse> convertir(
            @PathVariable Long cotizacionId, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaService.convertirCotizacion(cotizacionId, authentication));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<VentaResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody EstadoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ventaService.cambiarEstado(id, request.estado(), authentication));
    }
}
