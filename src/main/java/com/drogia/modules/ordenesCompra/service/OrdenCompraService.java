package com.drogia.modules.ordenesCompra.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.casos.service.CasoService;
import com.drogia.modules.inventario.dto.request.InventarioRequest;
import com.drogia.modules.inventario.service.InventarioService;
import com.drogia.modules.ordenesCompra.dto.request.OrdenCompraRequest;
import com.drogia.modules.ordenesCompra.dto.response.OrdenCompraResponse;
import com.drogia.modules.ordenesCompra.entity.OrdenCompra;
import com.drogia.modules.ordenesCompra.entity.enums.EstadoOrdenCompra;
import com.drogia.modules.ordenesCompra.repository.OrdenCompraRepository;
import com.drogia.modules.pedidosAlmacen.repository.PedidoAlmacenRepository;
import com.drogia.modules.pedidosAlmacen.service.PedidoAlmacenService;
import com.drogia.modules.proveedores.entity.Proveedor;
import com.drogia.modules.proveedores.repository.ProveedorRepository;
import com.drogia.modules.solicitudesCompra.entity.SolicitudCompra;
import com.drogia.modules.solicitudesCompra.repository.SolicitudCompraRepository;
import com.drogia.modules.solicitudesCompra.service.SolicitudCompraService;
import com.drogia.modules.ventas.entity.Venta;
import com.drogia.modules.ventas.repository.VentaRepository;
import com.drogia.shared.util.JsonUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de compras, recepción y traspaso a almacén. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrdenCompraService {

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    private final OrdenCompraRepository ordenRepository;
    private final ProveedorRepository proveedorRepository;
    private final SolicitudCompraRepository solicitudRepository;
    private final SolicitudCompraService solicitudService;
    private final InventarioService inventarioService;
    private final PedidoAlmacenService pedidoService;
    private final PedidoAlmacenRepository pedidoRepository;
    private final VentaRepository ventaRepository;
    private final CasoService casoService;
    private final ObjectMapper objectMapper;

    public List<OrdenCompraResponse> listar() {
        return ordenRepository.findAll(Sort.by(Sort.Direction.DESC, "fecha"))
                .stream().map(this::toResponse).toList();
    }

    public OrdenCompraResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Crea una orden de compra a partir de solicitudes pendientes. */
    public OrdenCompraResponse crear(OrdenCompraRequest request, Authentication authentication) {
        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));

        List<Long> solicitudIds = request.solicitudIds() == null || request.solicitudIds().isEmpty()
                ? solicitudRepository.findAllByEstado(
                        com.drogia.modules.solicitudesCompra.entity.enums.EstadoSolicitudCompra.PENDIENTE)
                        .stream().map(SolicitudCompra::getId).toList()
                : request.solicitudIds();

        List<OrdenCompraRequest.ItemRequest> items = request.items();
        if (items == null || items.isEmpty()) {
            items = solicitudIds.stream().map(this::itemDesdeSolicitud).toList();
        }
        BigDecimal subtotal = request.subtotal() != null
                ? request.subtotal()
                : items.stream().map(this::subtotalItem).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = request.igv() != null
                ? request.igv()
                : subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = request.total() != null ? request.total() : subtotal.add(igv);

        Long ventaId = solicitudIds.isEmpty() ? null
                : solicitudRepository.findById(solicitudIds.get(0))
                        .map(SolicitudCompra::getVentaId).orElse(null);
        Venta venta = ventaId == null ? null : ventaRepository.findById(ventaId).orElse(null);

        OrdenCompra orden = OrdenCompra.builder()
                .numero(generarNumero())
                .proveedorId(proveedor.getId())
                .proveedor(proveedor.getName())
                .casoId(request.casoId() != null ? request.casoId() : (venta == null ? null : venta.getCasoId()))
                .ventaId(ventaId)
                .ventaNumero(venta == null ? null : venta.getNumero())
                .fecha(request.date() == null ? LocalDate.now() : request.date())
                .itemsJson(JsonUtil.write(items))
                .solicitudesJson(JsonUtil.write(solicitudIds))
                .subtotal(subtotal)
                .igv(igv)
                .total(total)
                .estado(EstadoOrdenCompra.PENDIENTE)
                .creadoPor(authentication == null ? null : authentication.getName())
                .build();
        orden = ordenRepository.save(orden);
        solicitudService.marcarEnCompra(solicitudIds, orden.getId());
        if (venta != null && venta.getEstado().name().equals("PENDIENTE_COMPRA")) {
            venta.setEstado(com.drogia.modules.ventas.entity.enums.EstadoVenta.EN_PROCESO);
            ventaRepository.save(venta);
        }
        log.info("Orden de compra {} creada para proveedor {}", orden.getNumero(), proveedor.getName());
        return toResponse(orden);
    }

    public OrdenCompraResponse cambiarEstado(Long id, String estado) {
        OrdenCompra orden = buscar(id);
        orden.setEstado(EstadoOrdenCompra.from(estado));
        return toResponse(ordenRepository.save(orden));
    }

    /** Recepciona los lotes, incrementa inventario, cierra compras y crea el pedido de almacén. */
    public OrdenCompraResponse recepcionar(Long id) {
        OrdenCompra orden = buscar(id);
        if (orden.getEstado() == EstadoOrdenCompra.RECIBIDO
                || orden.getEstado() == EstadoOrdenCompra.CERRADO) {
            throw new ApiException(HttpStatus.CONFLICT, "La orden ya fue recibida");
        }
        List<OrdenCompraRequest.ItemRequest> items =
                JsonUtil.readList(orden.getItemsJson(), OrdenCompraRequest.ItemRequest.class);
        for (OrdenCompraRequest.ItemRequest item : items) {
            inventarioService.registrar(new InventarioRequest(
                    item.productId(), item.product(), orden.getNumero(), null,
                    item.quantity() == null ? 0 : item.quantity(), 10, "2027-12-31"));
        }

        List<Long> solicitudIds = JsonUtil.readList(orden.getSolicitudesJson(), Long.class);
        solicitudService.marcarRecibidas(orden.getId());
        casoService.marcarCerrado(orden.getCasoId());

        orden.setEstado(EstadoOrdenCompra.CERRADO);
        orden.setFechaRecepcion(LocalDateTime.now());
        orden = ordenRepository.save(orden);

        if (orden.getVentaId() != null
                && pedidoRepository.findByVentaId(orden.getVentaId()).isEmpty()) {
            pedidoService.crearDesdeOrden(orden);
        }
        log.info("Orden {} recibida; lotes registrados en almacén", orden.getNumero());
        return toResponse(orden);
    }

    private OrdenCompraRequest.ItemRequest itemDesdeSolicitud(Long solicitudId) {
        SolicitudCompra solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Solicitud de compra no encontrada: " + solicitudId));
        return new OrdenCompraRequest.ItemRequest(
                solicitud.getProductoId(), solicitud.getProducto(), "UNIDAD",
                solicitud.getCantidad(), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private BigDecimal subtotalItem(OrdenCompraRequest.ItemRequest item) {
        if (item.subtotal() != null && item.subtotal().compareTo(BigDecimal.ZERO) > 0) {
            return item.subtotal();
        }
        if (item.price() == null) {
            return BigDecimal.ZERO;
        }
        return item.price().multiply(BigDecimal.valueOf(item.quantity() == null ? 0 : item.quantity()));
    }

    private String generarNumero() {
        long siguiente = ordenRepository.count() + 1;
        String numero;
        do {
            numero = String.format("OC-%06d", siguiente++);
        } while (ordenRepository.existsByNumero(numero));
        return numero;
    }

    private OrdenCompra buscar(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Orden de compra no encontrada"));
    }

    private JsonNode supplier(OrdenCompra orden) {
        Proveedor proveedor = orden.getProveedorId() == null ? null
                : proveedorRepository.findById(orden.getProveedorId()).orElse(null);
        ObjectNode nodo = objectMapper.createObjectNode();
        nodo.put("id", proveedor == null ? 0 : proveedor.getId());
        nodo.put("name", proveedor == null ? orden.getProveedor() : proveedor.getName());
        nodo.put("document", proveedor == null ? "" : proveedor.getDocument());
        nodo.put("phone", proveedor == null ? "" : proveedor.getPhone());
        nodo.put("email", proveedor == null ? "" : proveedor.getEmail());
        return nodo;
    }

    private OrdenCompraResponse toResponse(OrdenCompra orden) {
        List<OrdenCompraRequest.ItemRequest> guardados =
                JsonUtil.readList(orden.getItemsJson(), OrdenCompraRequest.ItemRequest.class);
        List<OrdenCompraResponse.ItemResponse> items = guardados.stream()
                .map(item -> new OrdenCompraResponse.ItemResponse(
                        item.productId(), item.product(), item.unit(), item.quantity(),
                        item.price(), item.subtotal()))
                .toList();
        return new OrdenCompraResponse(
                orden.getId(), orden.getNumero(), supplier(orden),
                orden.getFecha() == null ? null : orden.getFecha().toString(),
                items, orden.getSubtotal(), orden.getIgv(), orden.getTotal(),
                orden.getEstado() == null ? null : orden.getEstado().label(),
                JsonUtil.readList(orden.getSolicitudesJson(), Long.class),
                orden.getVentaNumero(), orden.getCasoId());
    }
}
