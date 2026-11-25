package com.drogia.config;

import com.drogia.modules.casos.policy.CasoPermisos;
import com.drogia.modules.clientes.policy.ClientePermisos;
import com.drogia.modules.correos.policy.CorreoPermisos;
import com.drogia.modules.cotizaciones.policy.CotizacionPermisos;
import com.drogia.modules.despachos.policy.DespachoPermisos;
import com.drogia.modules.inventario.policy.InventarioPermisos;
import com.drogia.modules.ordenesCompra.policy.OrdenCompraPermisos;
import com.drogia.modules.pedidosAlmacen.policy.PedidoAlmacenPermisos;
import com.drogia.modules.productos.policy.ProductoPermisos;
import com.drogia.modules.proveedores.policy.ProveedorPermisos;
import com.drogia.modules.solicitudesCompra.policy.SolicitudCompraPermisos;
import com.drogia.modules.usuarios.policy.UsuarioPermisos;
import com.drogia.modules.ventas.policy.VentaPermisos;
import com.drogia.security.JwtAuthenticationFilter;
import com.drogia.security.JwtService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Configuración de seguridad HTTP con autenticación JWT. */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/correos/**").hasAnyRole(CorreoPermisos.rolesParaSecurity())
                        .requestMatchers("/api/usuarios/**").hasAnyRole(UsuarioPermisos.rolesParaSecurity())
                        .requestMatchers("/api/clientes/**").hasAnyRole(ClientePermisos.rolesParaSecurity())
                        .requestMatchers("/api/proveedores/**").hasAnyRole(ProveedorPermisos.rolesParaSecurity())
                        .requestMatchers(HttpMethod.GET, "/api/productos/**")
                        .hasAnyRole(ProductoPermisos.lecturaParaSecurity())
                        .requestMatchers("/api/productos/**")
                        .hasAnyRole(ProductoPermisos.escrituraParaSecurity())
                        .requestMatchers(HttpMethod.GET, "/api/ventas/**")
                        .hasAnyRole(VentaPermisos.lecturaParaSecurity())
                        .requestMatchers("/api/ventas/**")
                        .hasAnyRole(VentaPermisos.escrituraParaSecurity())
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes-compra/**")
                        .hasAnyRole(SolicitudCompraPermisos.lecturaParaSecurity())
                        .requestMatchers("/api/solicitudes-compra/**")
                        .hasAnyRole(SolicitudCompraPermisos.escrituraParaSecurity())
                        .requestMatchers(HttpMethod.GET, "/api/ordenes-compra/**")
                        .hasAnyRole(OrdenCompraPermisos.lecturaParaSecurity())
                        .requestMatchers("/api/ordenes-compra/**")
                        .hasAnyRole(OrdenCompraPermisos.escrituraParaSecurity())
                        .requestMatchers(HttpMethod.GET, "/api/inventario/**")
                        .hasAnyRole(InventarioPermisos.lecturaParaSecurity())
                        .requestMatchers("/api/inventario/**")
                        .hasAnyRole(InventarioPermisos.escrituraParaSecurity())
                        .requestMatchers(HttpMethod.GET, "/api/pedidos-almacen/**")
                        .hasAnyRole(PedidoAlmacenPermisos.lecturaParaSecurity())
                        .requestMatchers("/api/pedidos-almacen/**")
                        .hasAnyRole(PedidoAlmacenPermisos.escrituraParaSecurity())
                        .requestMatchers(HttpMethod.GET, "/api/despachos/**")
                        .hasAnyRole(DespachoPermisos.lecturaParaSecurity())
                        .requestMatchers("/api/despachos/**")
                        .hasAnyRole(DespachoPermisos.escrituraParaSecurity())
                        .requestMatchers(HttpMethod.POST, "/api/casos/*/tomar")
                        .hasAnyRole("VENTAS", "COMPRAS")
                        .requestMatchers(HttpMethod.PATCH, "/api/casos/*/estado")
                        .hasAnyRole("VENTAS", "COMPRAS")
                        .requestMatchers("/api/casos/**").hasAnyRole(CasoPermisos.rolesParaSecurity())
                        .requestMatchers(HttpMethod.GET, "/api/cotizaciones/**")
                        .hasAnyRole(CotizacionPermisos.lecturaParaSecurity())
                        .requestMatchers("/api/cotizaciones/**")
                        .hasAnyRole(CotizacionPermisos.escrituraParaSecurity())
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler((request, response, denied) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Acceso denegado\"}");
                        }));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private org.springframework.security.web.AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"No autenticado o token invalido\"}");
        };
    }
}
