package com.drogia.modules.clientes.repository;

import com.drogia.modules.clientes.entity.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de la entidad Cliente. */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocument(String document);
}
