package com.drogia.modules.cotizaciones.controller;

import com.drogia.modules.cotizaciones.dto.request.CotizacionRequest;
import com.drogia.modules.cotizaciones.dto.request.EnvioCotizacionRequest;
import com.drogia.modules.cotizaciones.dto.request.EstadoCotizacionRequest;
import com.drogia.modules.cotizaciones.dto.response.CotizacionResponse;
import com.drogia.modules.cotizaciones.service.CotizacionMailService;
import com.drogia.modules.cotizaciones.service.CotizacionService;
import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador del seguimiento de cotizaciones. */
@RestController
@RequestMapping("/api/cotizaciones")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;
    private final CotizacionMailService cotizacionMailService;

    @GetMapping
    public ResponseEntity<List<CotizacionResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(cotizacionService.listar(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CotizacionResponse> obtener(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(cotizacionService.obtener(id, authentication));
    }

    @PostMapping
    public ResponseEntity<CotizacionResponse> crear(
            @Valid @RequestBody CotizacionRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cotizacionService.crear(request, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CotizacionResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody CotizacionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(cotizacionService.actualizar(id, request, authentication));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CotizacionResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoCotizacionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(cotizacionService.cambiarEstado(id, request, authentication));
    }

    @GetMapping("/{id}/proveedores")
    public ResponseEntity<JsonNode> listarProveedores(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(cotizacionService.listarProveedores(id, authentication));
    }

    @PostMapping("/{id}/proveedores")
    public ResponseEntity<JsonNode> guardarProveedores(
            @PathVariable Long id, @RequestBody JsonNode proveedores,
            Authentication authentication) {
        return ResponseEntity.ok(cotizacionService.guardarProveedores(id, proveedores, authentication));
    }

    /** Envía la cotización al cliente con el PDF adjunto. */
    @PostMapping("/{id}/enviar-correo")
    public ResponseEntity<Map<String, Object>> enviarCorreo(
            @PathVariable Long id,
            @Valid @RequestBody EnvioCotizacionRequest request,
            Authentication authentication) {
        cotizacionMailService.enviar(request);
        CotizacionResponse cotizacion = cotizacionService.marcarEnviada(id, authentication);
        return ResponseEntity.ok(Map.of(
                "enviado", true,
                "mensaje", "Cotización " + cotizacion.number() + " enviada a " + request.to()));
    }
}
