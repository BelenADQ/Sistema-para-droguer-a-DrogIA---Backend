package com.drogia.modules.productos.repository;

import com.drogia.modules.productos.entity.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos del catálogo de productos. */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByNameContainingIgnoreCaseOrLotContainingIgnoreCaseOrHealthRegistrationContainingIgnoreCase(
            String name, String lot, String healthRegistration);
}
