package com.drogia.modules.despachos.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.casos.service.CasoService;
import com.drogia.modules.despachos.dto.request.DespachoRequest;
import com.drogia.modules.despachos.dto.response.DespachoResponse;
import com.drogia.modules.despachos.entity.Despacho;
import com.drogia.modules.despachos.entity.enums.EstadoDespacho;
import com.drogia.modules.despachos.repository.DespachoRepository;
import com.drogia.modules.pedidosAlmacen.entity.PedidoAlmacen;
import com.drogia.modules.pedidosAlmacen.entity.enums.TipoDespacho;
import com.drogia.modules.pedidosAlmacen.service.PedidoAlmacenService;
import com.drogia.modules.ventas.entity.Venta;
import com.drogia.modules.ventas.entity.enums.EstadoVenta;
import com.drogia.modules.ventas.repository.VentaRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de despachos locales y por courier. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final PedidoAlmacenService pedidoService;
    private final VentaRepository ventaRepository;
    private final CasoService casoService;

    public List<DespachoResponse> listar() {
        return despachoRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse).toList();
    }

    public DespachoResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    public DespachoResponse crear(DespachoRequest request) {
        PedidoAlmacen pedido = request.pedidoId() == null ? null
                : pedidoService.obtenerEntidad(request.pedidoId());
        TipoDespacho tipo = pedido == null ? TipoDespacho.LOCAL : pedido.getTipoDespacho();
        Venta venta = pedido == null ? null
                : ventaRepository.findById(pedido.getVentaId()).orElse(null);

        Despacho despacho = Despacho.builder()
                .code(generarCodigo())
                .pedidoId(request.pedidoId())
                .ventaId(venta == null ? null : venta.getId())
                .casoId(pedido == null ? null : pedido.getCasoId())
                .tipoDespacho(tipo)
                .thirdPartyName(tercerizador(request, tipo))
                .thirdPartyDoc(request.thirdPartyDoc())
                .courierEmpresa(request.courierEmpresa())
                .courierServicio(request.courierServicio())
                .numeroGuia(request.numeroGuia())
                .costoEnvio(request.costoEnvio())
                .product(request.product())
                .purchaseOrderCode(request.purchaseOrderCode())
                .quantity(request.quantity())
                .date(request.date())
                .status(EstadoDespacho.PROGRAMADO)
                .fechaCreacion(LocalDateTime.now())
                .build();
        despacho = despachoRepository.save(despacho);
        if (pedido != null) {
            pedidoService.vincularDespacho(pedido.getId(), despacho.getId());
            venta.setEstado(EstadoVenta.EN_DESPACHO);
            ventaRepository.save(venta);
        }
        log.info("Despacho {} creado tipo={}", despacho.getCode(), tipo);
        return toResponse(despacho);
    }

    /** Al confirmar la entrega se cierra la venta y el caso original. */
    public DespachoResponse cambiarEstado(Long id, String estado) {
        Despacho despacho = buscar(id);
        EstadoDespacho nuevoEstado = EstadoDespacho.from(estado);
        despacho.setStatus(nuevoEstado);
        if (nuevoEstado == EstadoDespacho.ENTREGADO) {
            despacho.setFechaEntrega(LocalDateTime.now());
            if (despacho.getPedidoId() != null) {
                pedidoService.marcarEntregado(despacho.getPedidoId());
            }
            if (despacho.getVentaId() != null) {
                Venta venta = ventaRepository.findById(despacho.getVentaId()).orElse(null);
                if (venta != null) {
                    venta.setEstado(EstadoVenta.ENTREGADA);
                    venta.setFechaEntrega(LocalDateTime.now());
                    ventaRepository.save(venta);
                    casoService.marcarCerrado(venta.getCasoId());
                }
            }
        }
        return toResponse(despachoRepository.save(despacho));
    }

    private String tercerizador(DespachoRequest request, TipoDespacho tipo) {
        if (request.thirdPartyName() != null && !request.thirdPartyName().isBlank()) {
            return request.thirdPartyName();
        }
        if (tipo == TipoDespacho.COURIER && request.courierEmpresa() != null) {
            return request.courierEmpresa();
        }
        return null;
    }

    private String generarCodigo() {
        long siguiente = despachoRepository.count() + 1;
        String codigo;
        do {
            codigo = String.format("DES-%06d", siguiente++);
        } while (despachoRepository.existsByCode(codigo));
        return codigo;
    }

    private Despacho buscar(Long id) {
        return despachoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Despacho no encontrado"));
    }

    private DespachoResponse toResponse(Despacho despacho) {
        return new DespachoResponse(
                despacho.getId(), despacho.getCode(), despacho.getPedidoId(), despacho.getVentaId(),
                despacho.getTipoDespacho() == null ? null : despacho.getTipoDespacho().name(),
                despacho.getThirdPartyName(), despacho.getThirdPartyDoc(), despacho.getProduct(),
                despacho.getPurchaseOrderCode(), despacho.getQuantity(), despacho.getDate(),
                despacho.getStatus() == null ? null : despacho.getStatus().label(),
                despacho.getCourierEmpresa(), despacho.getCourierServicio(), despacho.getNumeroGuia(),
                despacho.getCostoEnvio());
    }
}
