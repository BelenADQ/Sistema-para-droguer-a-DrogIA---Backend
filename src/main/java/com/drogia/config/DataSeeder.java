package com.drogia.config;

import com.drogia.entities.User;
import com.drogia.entities.enums.Rol;
import com.drogia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Carga los usuarios iniciales si la tabla usuarios está vacía. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        create("admin", "123456", "Administrador", Rol.ADMINISTRACION);
        create("vendedor", "123456", "Carlos Mendoza", Rol.VENTAS);
        create("comprador", "123456", "Ana Torres", Rol.COMPRAS);
        create("almacenero", "123456", "Luis Ramos", Rol.ALMACEN);
        create("admin_test", "Admin2026", "Administrador Prueba", Rol.ADMINISTRACION);
        create("ventas_test", "Ventas2026", "Vendedor Prueba", Rol.VENTAS);
        create("compras_test", "Compras2026", "Comprador Prueba", Rol.COMPRAS);
        create("almacen_test", "Almacen2026", "Almacenero Prueba", Rol.ALMACEN);
    }

    private void create(String username, String password, String nombre, Rol rol) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nombre(nombre)
                .rol(rol)
                .enabled(true)
                .build());
    }
}
