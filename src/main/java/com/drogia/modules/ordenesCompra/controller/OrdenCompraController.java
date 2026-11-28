package com.drogia.modules.ordenesCompra.controller;

import com.drogia.modules.ordenesCompra.dto.request.OrdenCompraRequest;
import com.drogia.modules.ordenesCompra.dto.response.OrdenCompraResponse;
import com.drogia.modules.ordenesCompra.service.OrdenCompraService;
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

/** Controlador de órdenes de compra. */
@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenService;

    @GetMapping
    public ResponseEntity<List<OrdenCompraResponse>> listar() {
        return ResponseEntity.ok(ordenService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<OrdenCompraResponse> crear(
            @Valid @RequestBody OrdenCompraRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.crear(request, authentication));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<OrdenCompraResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(ordenService.cambiarEstado(id, request.estado()));
    }

    /** Registra la recepción en almacén y cierra el proceso de compras. */
    @PostMapping("/{id}/recepcion")
    public ResponseEntity<OrdenCompraResponse> recepcionar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.recepcionar(id));
    }
}
