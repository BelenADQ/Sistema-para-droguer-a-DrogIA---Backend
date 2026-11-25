package com.drogia.modules.solicitudesCompra.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.solicitudesCompra.dto.response.SolicitudCompraResponse;
import com.drogia.modules.solicitudesCompra.entity.SolicitudCompra;
import com.drogia.modules.solicitudesCompra.entity.enums.EstadoSolicitudCompra;
import com.drogia.modules.solicitudesCompra.repository.SolicitudCompraRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de solicitudes de compra derivadas de ventas. */
@Service
@RequiredArgsConstructor
@Transactional
public class SolicitudCompraService {

    private final SolicitudCompraRepository solicitudCompraRepository;

    public List<SolicitudCompraResponse> listar() {
        return solicitudCompraRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion"))
                .stream().map(this::toResponse).toList();
    }

    public SolicitudCompraResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    public SolicitudCompraResponse cambiarEstado(Long id, String estado) {
        SolicitudCompra solicitud = buscar(id);
        solicitud.setEstado(EstadoSolicitudCompra.from(estado));
        return toResponse(solicitudCompraRepository.save(solicitud));
    }

    /** Marca las solicitudes usadas en una orden como "En compra". */
    public void marcarEnCompra(List<Long> ids, Long ordenCompraId) {
        for (Long id : ids) {
            SolicitudCompra solicitud = buscar(id);
            solicitud.setEstado(EstadoSolicitudCompra.EN_COMPRA);
            solicitud.setOrdenCompraId(ordenCompraId);
            solicitudCompraRepository.save(solicitud);
        }
    }

    /** Marca como recibidas las solicitudes asociadas a una orden. */
    public void marcarRecibidas(Long ordenCompraId) {
        for (SolicitudCompra solicitud : solicitudCompraRepository.findAllByOrdenCompraId(ordenCompraId)) {
            solicitud.setEstado(EstadoSolicitudCompra.RECIBIDO);
            solicitudCompraRepository.save(solicitud);
        }
    }

    private SolicitudCompra buscar(Long id) {
        return solicitudCompraRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de compra no encontrada"));
    }

    private SolicitudCompraResponse toResponse(SolicitudCompra solicitud) {
        return new SolicitudCompraResponse(
                solicitud.getId(), solicitud.getVentaId(), solicitud.getVentaNumero(),
                solicitud.getProductoId(), solicitud.getProducto(), solicitud.getCantidad(),
                solicitud.getEstado() == null ? null : solicitud.getEstado().label(),
                solicitud.getOrdenCompraId());
    }
}
