package com.drogia.security;

import com.drogia.entities.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/** Generación y validación de tokens JWT. */
@Service
public class JwtService {

    private static final String ISSUER = "drogia-api";
    private static final String ROL_CLAIM = "rol";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long expirationMs;

    /** Construye el codificador y el decodificador con la configuración dada. */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos 32 bytes (256 bits) para HS256");
        }
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        this.decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        this.expirationMs = expirationMs;
    }

    /** Genera el token firmado para un usuario, incluyendo su rol. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plusMillis(expirationMs))
                .claim(ROL_CLAIM, user.getRol().name())
                .build();
        JwtEncoderParameters parameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims);
        return encoder.encode(parameters).getTokenValue();
    }

    /** Lee el subject del token (el username). Lanza si la firma/expiración no valida. */
    public String extractUsername(String token) {
        return decoder.decode(token).getSubject();
    }

    /** Lee el claim "rol" del token (nombre del rol del usuario). */
    public String extractRol(String token) {
        return decoder.decode(token).getClaimAsString(ROL_CLAIM);
    }

    /** Expiracion en segundos (para informarla en la respuesta del login). */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
