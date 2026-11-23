package com.drogia.repository;

import com.drogia.entities.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByCorreoIdOrderByIdAsc(Long correoId);
}
