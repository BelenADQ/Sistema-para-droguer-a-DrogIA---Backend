package com.drogia.modules.casos.repository;

import com.drogia.modules.casos.entity.Caso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de los casos de negocio. */
public interface CasoRepository extends JpaRepository<Caso, Long> {

    boolean existsByCorreoId(Long correoId);

    boolean existsByCodigoSolicitud(String codigoSolicitud);

    Optional<Caso> findByCorreoId(Long correoId);

    List<Caso> findAllByArea(String area, Sort sort);

    List<Caso> findAllByAsignadoAId(Long asignadoAId, Sort sort);
}
