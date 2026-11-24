package com.drogia.modules.correos.repository;

import com.drogia.modules.correos.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByCorreoIdOrderByIdAsc(Long correoId);
}
