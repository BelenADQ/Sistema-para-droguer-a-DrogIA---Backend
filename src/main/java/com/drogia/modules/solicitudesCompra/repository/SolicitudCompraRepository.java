package com.drogia.modules.solicitudesCompra.repository;

import com.drogia.modules.solicitudesCompra.entity.SolicitudCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de solicitudes de compra. */
public interface SolicitudCompraRepository extends JpaRepository<SolicitudCompra, Long> {

    List<SolicitudCompra> findAllByVentaId(Long ventaId);

    List<SolicitudCompra> findAllByOrdenCompraId(Long ordenCompraId);

    List<SolicitudCompra> findAllByEstado(com.drogia.modules.solicitudesCompra.entity.enums.EstadoSolicitudCompra estado);
}
