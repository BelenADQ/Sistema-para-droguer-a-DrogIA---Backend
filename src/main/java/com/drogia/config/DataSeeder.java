package com.drogia.config;

import com.drogia.modules.clientes.entity.Cliente;
import com.drogia.modules.clientes.repository.ClienteRepository;
import com.drogia.modules.productos.entity.Producto;
import com.drogia.modules.productos.repository.ProductoRepository;
import com.drogia.modules.proveedores.entity.Proveedor;
import com.drogia.modules.proveedores.repository.ProveedorRepository;
import com.drogia.modules.usuarios.entity.User;
import com.drogia.modules.usuarios.repository.UserRepository;
import com.drogia.shared.enums.Rol;
import java.math.BigDecimal;
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
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsuarios();
        seedClientes();
        seedProveedores();
        seedProductos();
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
        crearCliente("Rodríguez S.A.C.", "20123456789", "compras@rodriguez.com");
        crearCliente("Distribuidora Salud Perú", "20456789123", "pedidos@saludperu.com");
        crearCliente("Farmacias del Sur", "20678912345", "contacto@farmaciasdelsur.com");
    }

    private void crearCliente(String nombre, String documento, String email) {
        Cliente cliente = clienteRepository.findByDocument(documento).orElse(null);
        if (cliente == null) {
            clienteRepository.save(Cliente.builder()
                    .name(nombre)
                    .document(documento)
                    .email(email)
                    .build());
            return;
        }
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            cliente.setEmail(email);
            clienteRepository.save(cliente);
        }
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

    private void seedProductos() {
        if (productoRepository.count() > 0) {
            return;
        }
        crearProducto("Paracetamol 500 mg", "CAJA", "LOT-PAR-001", "RS-001-2026",
                new BigDecimal("12.50"), 120, "2027-08-15");
        crearProducto("Ibuprofeno 400 mg", "CAJA", "LOT-IBU-001", "RS-002-2026",
                new BigDecimal("15.80"), 80, "2027-09-20");
        crearProducto("Amoxicilina 500 mg", "CAJA", "LOT-AMO-001", "RS-003-2026",
                new BigDecimal("22.40"), 60, "2027-10-10");
        crearProducto("Suero fisiológico 0.9% 500 ml", "UNIDAD", "LOT-SUE-001", "RS-004-2026",
                new BigDecimal("8.90"), 200, "2028-01-30");
        crearProducto("Vitamina C 1 g", "CAJA", "LOT-VIT-001", "RS-005-2026",
                new BigDecimal("10.20"), 150, "2027-11-25");
    }

    private void crearProducto(String nombre, String unidad, String lote,
            String registroSanitario, BigDecimal precio, Integer stock, String vencimiento) {
        productoRepository.save(Producto.builder()
                .name(nombre)
                .unit(unidad)
                .lot(lote)
                .healthRegistration(registroSanitario)
                .price(precio)
                .stock(stock)
                .expiration(vencimiento)
                .build());
    }
}
