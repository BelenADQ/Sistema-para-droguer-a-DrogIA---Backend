package com.drogia.modules.casos.controller;

import com.drogia.modules.casos.dto.request.EstadoCasoRequest;
import com.drogia.modules.casos.dto.response.CasoResponse;
import com.drogia.modules.casos.service.CasoService;
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

/** Controlador del seguimiento de casos por área. */
@RestController
@RequestMapping("/api/casos")
@RequiredArgsConstructor
public class CasoController {

    private final CasoService casoService;

    /** Lista los casos permitidos para el rol autenticado. */
    @GetMapping
    public ResponseEntity<List<CasoResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(casoService.listar(authentication));
    }

    /** Devuelve un caso por identificador. */
    @GetMapping("/{id}")
    public ResponseEntity<CasoResponse> obtener(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(casoService.obtener(id, authentication));
    }

    /** El usuario de Ventas o Compras toma el caso de su área. */
    @PostMapping("/{id}/tomar")
    public ResponseEntity<CasoResponse> tomar(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(casoService.tomar(id, authentication));
    }

    /** Actualiza manualmente el estado del caso asignado. */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<CasoResponse> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoCasoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(casoService.actualizarEstado(id, request, authentication));
    }
}
