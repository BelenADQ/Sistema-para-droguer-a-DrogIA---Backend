package com.drogia.dto.request;

import com.drogia.entities.enums.EstadoCorreo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cuerpo de la petición PATCH de un correo. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCorreoRequest {
    @NotNull(message = "el estado es obligatorio")
    private EstadoCorreo estado;
    private String area;
}