package com.drogia.modules.pedidosAlmacen.controller;

import com.drogia.modules.pedidosAlmacen.dto.request.VerificacionPedidoRequest;
import com.drogia.modules.pedidosAlmacen.dto.response.InventarioAlmacenResponse;
import com.drogia.modules.pedidosAlmacen.dto.response.PedidoAlmacenResponse;
import com.drogia.modules.pedidosAlmacen.service.InventarioAlmacenService;
import com.drogia.modules.pedidosAlmacen.service.PedidoAlmacenService;
import com.drogia.shared.dto.EstadoRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador de pedidos en almacén. */
@RestController
@RequestMapping("/api/pedidos-almacen")
@RequiredArgsConstructor
public class PedidoAlmacenController {

    private final PedidoAlmacenService pedidoService;
    private final InventarioAlmacenService inventarioAlmacenService;

    @GetMapping("/inventario-pendiente")
    public ResponseEntity<List<InventarioAlmacenResponse>> listarInventarioPendiente() {
        return ResponseEntity.ok(inventarioAlmacenService.listarPendientes());
    }

    @GetMapping
    public ResponseEntity<List<PedidoAlmacenResponse>> listar() {
        return ResponseEntity.ok(pedidoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoAlmacenResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtener(id));
    }

    @PostMapping("/{id}/tomar")
    public ResponseEntity<PedidoAlmacenResponse> tomar(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(pedidoService.tomar(id, authentication));
    }

    @PatchMapping("/{id}/verificacion")
    public ResponseEntity<PedidoAlmacenResponse> verificar(
            @PathVariable Long id, @Valid @RequestBody VerificacionPedidoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(pedidoService.verificar(id, request, authentication));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoAlmacenResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody EstadoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, request.estado(), authentication));
    }

    @PostMapping("/{id}/despacho/{despachoId}")
    public ResponseEntity<PedidoAlmacenResponse> vincularDespacho(
            @PathVariable Long id, @PathVariable Long despachoId) {
        pedidoService.vincularDespacho(id, despachoId);
        return ResponseEntity.ok(pedidoService.obtener(id));
    }
}
