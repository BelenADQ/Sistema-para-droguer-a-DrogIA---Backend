package com.drogia.modules.inventario.controller;

import com.drogia.modules.inventario.dto.request.InventarioRequest;
import com.drogia.modules.inventario.dto.response.InventarioResponse;
import com.drogia.modules.inventario.service.InventarioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador de inventario. */
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping
    public ResponseEntity<List<InventarioResponse>> listar() {
        return ResponseEntity.ok(inventarioService.listar());
    }

    @PostMapping
    public ResponseEntity<InventarioResponse> crear(@Valid @RequestBody InventarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventarioResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody InventarioRequest request) {
        return ResponseEntity.ok(inventarioService.actualizar(id, request));
    }
}
