package com.drogia.modules.despachos.controller;

import com.drogia.modules.despachos.dto.request.DespachoRequest;
import com.drogia.modules.despachos.dto.response.DespachoResponse;
import com.drogia.modules.despachos.service.DespachoService;
import com.drogia.shared.dto.EstadoRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador de despachos. */
@RestController
@RequestMapping("/api/despachos")
@RequiredArgsConstructor
public class DespachoController {

    private final DespachoService despachoService;

    @GetMapping
    public ResponseEntity<List<DespachoResponse>> listar() {
        return ResponseEntity.ok(despachoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DespachoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(despachoService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<DespachoResponse> crear(@Valid @RequestBody DespachoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(despachoService.crear(request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<DespachoResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(despachoService.cambiarEstado(id, request.estado()));
    }
}
