package com.drogia.modules.ventas.repository;

import com.drogia.modules.ventas.entity.Venta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de ventas. */
public interface VentaRepository extends JpaRepository<Venta, Long> {

    boolean existsByNumero(String numero);

    Optional<Venta> findByCotizacionId(Long cotizacionId);

    List<Venta> findAll(Sort sort);
}
