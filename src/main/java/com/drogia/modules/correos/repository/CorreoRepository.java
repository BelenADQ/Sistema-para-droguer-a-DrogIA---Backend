package com.drogia.modules.correos.repository;

import com.drogia.modules.correos.entity.Correo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de la entidad Correo. */
public interface CorreoRepository extends JpaRepository<Correo, Long> {
    Optional<Correo> findByMessageId(String messageId);

    List<Correo> findAllByArea(String area, Sort sort);
}