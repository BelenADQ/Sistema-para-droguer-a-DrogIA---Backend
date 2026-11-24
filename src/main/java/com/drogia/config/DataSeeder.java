package com.drogia.config;

import com.drogia.modules.clientes.entity.Cliente;
import com.drogia.modules.clientes.repository.ClienteRepository;
import com.drogia.modules.proveedores.entity.Proveedor;
import com.drogia.modules.proveedores.repository.ProveedorRepository;
import com.drogia.modules.usuarios.entity.User;
import com.drogia.modules.usuarios.repository.UserRepository;
import com.drogia.shared.enums.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Carga los datos iniciales de usuarios, clientes y proveedores. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsuarios();
        seedClientes();
        seedProveedores();
    }

    private void seedUsuarios() {
        crearUsuario("admin", "admin@drogia.com", "123456", "Administrador", Rol.ADMINISTRACION);
        crearUsuario("vendedor", "vendedor@drogia.com", "123456", "Carlos Mendoza", Rol.VENTAS);
        crearUsuario("comprador", "comprador@drogia.com", "123456", "Ana Torres", Rol.COMPRAS);
        crearUsuario("almacenero", "almacenero@drogia.com", "123456", "Luis Ramos", Rol.ALMACEN);

        crearUsuario("admin_test", "admin_test@drogia.com", "Admin2026", "Administrador Prueba", Rol.ADMINISTRACION);
        crearUsuario("ventas_test", "ventas_test@drogia.com", "Ventas2026", "Vendedor Prueba", Rol.VENTAS);
        crearUsuario("compras_test", "compras_test@drogia.com", "Compras2026", "Comprador Prueba", Rol.COMPRAS);
        crearUsuario("almacen_test", "almacen_test@drogia.com", "Almacen2026", "Almacenero Prueba", Rol.ALMACEN);
    }

    private void crearUsuario(String username, String email, String password, String nombre, Rol rol) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            userRepository.save(User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .nombre(nombre)
                    .rol(rol)
                    .enabled(true)
                    .build());
            return;
        }
        if ((user.getEmail() == null || user.getEmail().isBlank())
                && userRepository.findByEmail(email).isEmpty()) {
            user.setEmail(email);
            userRepository.save(user);
        }
    }

    private void seedClientes() {
        if (clienteRepository.count() > 0) {
            return;
        }
        clienteRepository.save(Cliente.builder().name("Rodríguez S.A.C.").document("20123456789").build());
        clienteRepository.save(Cliente.builder().name("Distribuidora Salud Perú").document("20456789123").build());
        clienteRepository.save(Cliente.builder().name("Farmacias del Sur").document("20678912345").build());
    }

    private void seedProveedores() {
        if (proveedorRepository.count() > 0) {
            return;
        }
        proveedorRepository.save(Proveedor.builder()
                .name("Laboratorios del Sur S.A.C.")
                .document("20123456789")
                .phone("987654321")
                .email("contacto@labsur.com")
                .build());
        proveedorRepository.save(Proveedor.builder()
                .name("Distribuidora Médica Perú S.A.C.")
                .document("20456789123")
                .phone("956123456")
                .email("ventas@distribuidoramedica.com")
                .build());
        proveedorRepository.save(Proveedor.builder()
                .name("Productos Farmacéuticos Andinos")
                .document("20678912345")
                .phone("945789123")
                .email("contacto@farmaandinos.com")
                .build());
    }
}
