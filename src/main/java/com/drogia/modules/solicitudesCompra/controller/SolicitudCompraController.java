package com.drogia.modules.solicitudesCompra.controller;

import com.drogia.modules.solicitudesCompra.dto.response.SolicitudCompraResponse;
import com.drogia.modules.solicitudesCompra.service.SolicitudCompraService;
import com.drogia.shared.dto.EstadoRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador de solicitudes de compra. */
@RestController
@RequestMapping("/api/solicitudes-compra")
@RequiredArgsConstructor
public class SolicitudCompraController {

    private final SolicitudCompraService solicitudCompraService;

    @GetMapping
    public ResponseEntity<List<SolicitudCompraResponse>> listar() {
        return ResponseEntity.ok(solicitudCompraService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudCompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudCompraService.obtener(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<SolicitudCompraResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(solicitudCompraService.cambiarEstado(id, request.estado()));
    }
}
