package com.drogia.modules.cotizaciones.repository;

import com.drogia.modules.cotizaciones.entity.Cotizacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de cotizaciones. */
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    boolean existsByNumero(String numero);

    Optional<Cotizacion> findByCasoId(Long casoId);

    List<Cotizacion> findAllByCreadoPorId(Long creadoPorId, Sort sort);
}
