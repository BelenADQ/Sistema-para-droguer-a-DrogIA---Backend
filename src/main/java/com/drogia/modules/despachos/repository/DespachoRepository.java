package com.drogia.modules.despachos.repository;

import com.drogia.modules.despachos.entity.Despacho;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de despachos. */
public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    boolean existsByCode(String code);

    Optional<Despacho> findByPedidoId(Long pedidoId);

    List<Despacho> findAllByOrderByIdDesc();
}
