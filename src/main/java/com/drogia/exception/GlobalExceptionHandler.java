package com.drogia.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejo centralizado de excepciones de la API. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Credenciales incorrectas en el login -> 401. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Credenciales invalidas: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
    }

    /** Cuenta deshabilitada en el login -> 401. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(DisabledException ex) {
        log.warn("Usuario deshabilitado: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Usuario deshabilitado");
    }

    /** Fallo de validación de los DTO (@Valid) -> 400 con los campos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Datos invalidos: {}", ex.getMessage());
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(field -> fields.put(field.getField(), field.getDefaultMessage()));
        Map<String, String> body = new HashMap<>();
        body.put("message", "Datos invalidos");
        body.putAll(fields);
        return ResponseEntity.badRequest().body(body);
    }

    /** Cuerpo JSON ilegible / mal formado -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de la peticion invalido: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Cuerpo de la peticion invalido");
    }

    /** Error con el servicio IMAP de Gmail -> 502 (upstream). */
    @ExceptionHandler(com.drogia.exception.GmailException.class)
    public ResponseEntity<Map<String, String>> handleGmail(
            com.drogia.exception.GmailException ex) {
        log.warn("Error Gmail/IMAP: {}", ex.getMessage());
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(com.drogia.exception.ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(com.drogia.exception.ApiException ex) {
        return error(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Helper: respuesta JSON {"message": ...} con el código HTTP dado. */
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
