package com.drogia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Respuesta del login con el token JWT y los datos del usuario. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String type;
    private String username;
    private String name;
    private String role;
    private String rol;
    private long expiresIn;
}
