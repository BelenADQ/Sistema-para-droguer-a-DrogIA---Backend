package com.drogia.modules.ordenesCompra.repository;

import com.drogia.modules.ordenesCompra.entity.OrdenCompra;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de órdenes de compra. */
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    boolean existsByNumero(String numero);

    List<OrdenCompra> findAll(Sort sort);
}
