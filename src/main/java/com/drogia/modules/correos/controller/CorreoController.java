package com.drogia.modules.correos.controller;

import com.drogia.modules.correos.dto.request.EstadoCorreoRequest;
import com.drogia.modules.correos.dto.response.CorreoResponse;
import com.drogia.modules.correos.policy.CorreoPermisos;
import com.drogia.modules.correos.service.CorreoService;
import com.drogia.security.RolResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Controlador de los endpoints del módulo de correos. */
@RestController
@RequestMapping("/api/correos")
@RequiredArgsConstructor
public class CorreoController {

    private final CorreoService correoService;

    /** Lista las carpetas disponibles en la cuenta de Gmail. */
    @GetMapping("/carpetas")
    public ResponseEntity<List<String>> carpetas() {
        return ResponseEntity.ok(correoService.carpetas());
    }

    /** Lee en vivo una carpeta de Gmail (INBOX por defecto) y la persiste. */
    @GetMapping
    public ResponseEntity<List<CorreoResponse>> listar(
            @RequestParam(defaultValue = "INBOX") String carpeta,
            @RequestParam(defaultValue = "20") int limite,
            Authentication authentication) {
        return ResponseEntity.ok(correoService.listarEnVivo(carpeta, limite, areaDelRol(authentication)));
    }

    /** Devuelve los correos permitidos según el área del usuario. */
    @GetMapping("/historial")
    public ResponseEntity<List<CorreoResponse>> historial(Authentication authentication) {
        return ResponseEntity.ok(correoService.historial(areaDelRol(authentication)));
    }

    /** Actualiza estado (y opcionalmente área) de un correo persistido. */
    @PatchMapping("/{id}")
    public ResponseEntity<CorreoResponse> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoCorreoRequest request) {
        return ResponseEntity.ok(
                correoService.actualizarEstado(id, request.getEstado(), request.getArea()));
    }

    @PostMapping("/{id}/clasificar")
    public ResponseEntity<CorreoResponse> clasificar(@PathVariable Long id) {
        return ResponseEntity.ok(correoService.clasificar(id));
    }

    @PostMapping("/ingesta/ejecutar")
    public ResponseEntity<Map<String, Object>> ejecutarIngesta() {
        return ResponseEntity.ok(Map.of("procesados", correoService.ejecutarIngesta()));
    }

    /** Devuelve el área permitida según el rol del token. */
    private String areaDelRol(Authentication authentication) {
        return CorreoPermisos.areaPorRol(RolResolver.from(authentication));
    }
}