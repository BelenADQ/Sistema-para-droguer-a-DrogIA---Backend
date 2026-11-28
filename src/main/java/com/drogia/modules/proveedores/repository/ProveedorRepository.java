package com.drogia.modules.proveedores.repository;

import com.drogia.modules.proveedores.entity.Proveedor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de la entidad Proveedor. */
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByDocument(String document);
}
