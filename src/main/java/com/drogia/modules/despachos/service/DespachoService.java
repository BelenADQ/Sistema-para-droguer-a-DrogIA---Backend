package com.drogia.modules.despachos.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.casos.service.CasoService;
import com.drogia.modules.despachos.dto.request.DespachoRequest;
import com.drogia.modules.despachos.dto.response.DespachoResponse;
import com.drogia.modules.despachos.entity.Despacho;
import com.drogia.modules.despachos.entity.enums.EstadoDespacho;
import com.drogia.modules.despachos.repository.DespachoRepository;
import com.drogia.modules.inventario.service.InventarioService;
import com.drogia.modules.ordenesCompra.entity.OrdenCompra;
import com.drogia.modules.ordenesCompra.repository.OrdenCompraRepository;
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
import tools.jackson.databind.JsonNode;

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
    private final InventarioService inventarioService;
    private final OrdenCompraRepository ordenRepository;

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
        String codigoOrden = codigoOrdenDespacho(request, pedido);
        OrdenCompra orden = codigoOrden == null ? null
                : ordenRepository.findByNumero(codigoOrden).orElse(null);

        Long ventaId = pedido != null ? pedido.getVentaId()
                : (orden == null ? null : orden.getVentaId());
        Long casoId = pedido != null ? pedido.getCasoId()
                : (orden == null ? null : orden.getCasoId());
        Venta venta = ventaId == null ? null : ventaRepository.findById(ventaId).orElse(null);
        TipoDespacho tipo = tipoDespacho(pedido, orden);

        Despacho despacho = Despacho.builder()
                .code(generarCodigo())
                .pedidoId(request.pedidoId())
                .ventaId(venta == null ? null : venta.getId())
                .casoId(casoId)
                .tipoDespacho(tipo)
                .thirdPartyName(tercerizador(request, tipo))
                .thirdPartyDoc(request.thirdPartyDoc())
                .courierEmpresa(request.courierEmpresa())
                .courierServicio(request.courierServicio())
                .numeroGuia(request.numeroGuia())
                .costoEnvio(request.costoEnvio())
                .product(request.product())
                .purchaseOrderCode(codigoOrden != null ? codigoOrden : request.purchaseOrderCode())
                .quantity(request.quantity())
                .date(request.date())
                .status(EstadoDespacho.PROGRAMADO)
                .fechaCreacion(LocalDateTime.now())
                .build();
        despacho = despachoRepository.save(despacho);

        if (pedido != null) {
            pedidoService.vincularDespacho(pedido.getId(), despacho.getId());
        }
        inventarioService.vaciarPorOrden(codigoOrden);
        if (venta != null) {
            venta.setEstado(EstadoVenta.EN_DESPACHO);
            ventaRepository.save(venta);
        }

        log.info("Despacho {} creado tipo={} orden={}", despacho.getCode(), tipo, codigoOrden);
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
            } else if (despacho.getCasoId() != null) {
                casoService.marcarCerrado(despacho.getCasoId());
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

    private TipoDespacho tipoDespacho(PedidoAlmacen pedido, OrdenCompra orden) {
        if (pedido != null && pedido.getTipoDespacho() != null) {
            return pedido.getTipoDespacho();
        }
        JsonNode destino = orden == null ? null : pedidoService.destinoDe(orden.getCasoId());
        String departamento = destino == null ? "" : destino.path("departamento").asText("");
        String provincia = destino == null ? "" : destino.path("provincia").asText("");
        if ("Arequipa".equalsIgnoreCase(departamento)
                && (provincia.isBlank() || "Arequipa".equalsIgnoreCase(provincia))) {
            return TipoDespacho.LOCAL;
        }
        return TipoDespacho.COURIER;
    }

    private String codigoOrdenDespacho(DespachoRequest request, PedidoAlmacen pedido) {
        if (pedido != null && pedido.getOrdenCompraId() != null) {
            String numero = ordenRepository.findById(pedido.getOrdenCompraId())
                    .map(OrdenCompra::getNumero)
                    .orElse(null);
            if (numero != null && !numero.isBlank()) {
                return numero;
            }
        }
        if (request.purchaseOrderCode() != null && !request.purchaseOrderCode().isBlank()) {
            return request.purchaseOrderCode().trim();
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
