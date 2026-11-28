package com.drogia.modules.ventas.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.casos.service.CasoService;
import com.drogia.modules.cotizaciones.dto.request.CotizacionRequest;
import com.drogia.modules.cotizaciones.entity.Cotizacion;
import com.drogia.modules.cotizaciones.entity.enums.EstadoCotizacion;
import com.drogia.modules.cotizaciones.repository.CotizacionRepository;
import com.drogia.modules.solicitudesCompra.entity.SolicitudCompra;
import com.drogia.modules.solicitudesCompra.entity.enums.EstadoSolicitudCompra;
import com.drogia.modules.solicitudesCompra.repository.SolicitudCompraRepository;
import com.drogia.modules.ventas.dto.request.VentaRequest;
import com.drogia.modules.ventas.dto.response.VentaResponse;
import com.drogia.modules.ventas.entity.Venta;
import com.drogia.modules.ventas.entity.enums.EstadoVenta;
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

/** Servicio de ventas y generación de solicitudes de compra. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VentaService {

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    private final VentaRepository ventaRepository;
    private final CotizacionRepository cotizacionRepository;
    private final SolicitudCompraRepository solicitudCompraRepository;
    private final CasoService casoService;
    private final ObjectMapper objectMapper;

    /** Lista todas las ventas. */
    public List<VentaResponse> listar() {
        return ventaRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion"))
                .stream().map(this::toResponse).toList();
    }

    public VentaResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Convierte una cotización aceptada en venta y crea sus solicitudes de compra. */
    public VentaResponse convertirCotizacion(Long cotizacionId, Authentication authentication) {
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cotización no encontrada"));
        if (ventaRepository.findByCotizacionId(cotizacionId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "La cotización ya fue convertida en venta");
        }
        if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA
                && cotizacion.getEstado() != EstadoCotizacion.ENVIADA) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La cotización debe estar aceptada para convertirse en venta");
        }

        List<CotizacionRequest.ItemRequest> itemsCotizacion =
                JsonUtil.readList(cotizacion.getItemsJson(), CotizacionRequest.ItemRequest.class);
        List<VentaRequest.ItemRequest> items = itemsCotizacion.stream()
                .map(item -> new VentaRequest.ItemRequest(
                        item.productoId(), item.producto(), item.unidad(), item.cantidad(),
                        item.precioUnitario(), item.subtotal()))
                .toList();
        BigDecimal subtotal = cotizacion.getSubtotal() != null
                ? cotizacion.getSubtotal()
                : items.stream().map(this::subtotalItem).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = cotizacion.getIgv() != null
                ? cotizacion.getIgv()
                : subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = cotizacion.getTotal() != null
                ? cotizacion.getTotal()
                : subtotal.add(igv);

        JsonNode cliente = clienteDe(cotizacion);
        String creadoPor = authentication == null ? null : authentication.getName();
        Venta venta = Venta.builder()
                .numero(generarNumero())
                .cotizacionId(cotizacion.getId())
                .cotizacionNumero(cotizacion.getNumero())
                .casoId(cotizacion.getCasoId())
                .clienteId(cotizacion.getClienteId())
                .clienteJson(JsonUtil.write(cliente))
                .fecha(LocalDate.now())
                .itemsJson(JsonUtil.write(items))
                .subtotal(subtotal)
                .igv(igv)
                .total(total)
                .estado(EstadoVenta.PENDIENTE_COMPRA)
                .creadoPor(creadoPor)
                .fechaCreacion(LocalDateTime.now())
                .build();
        venta = ventaRepository.save(venta);
        crearSolicitudes(venta, items);

        cotizacion.setEstado(EstadoCotizacion.CONVERTIDA);
        cotizacionRepository.save(cotizacion);
        casoService.marcarConvertido(cotizacion.getCasoId());
        log.info("Venta {} creada desde cotización {}", venta.getNumero(), cotizacion.getNumero());
        return toResponse(venta);
    }

    /** Registro directo de una venta, sin cotización previa. */
    public VentaResponse crear(VentaRequest request, Authentication authentication) {
        List<VentaRequest.ItemRequest> items = request.items() == null ? List.of() : request.items();
        BigDecimal subtotal = request.subtotal() != null
                ? request.subtotal()
                : items.stream().map(item -> valor(item.subtotal(), item.price(), item.quantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = request.igv() != null
                ? request.igv()
                : subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = request.total() != null ? request.total() : subtotal.add(igv);
        LocalDate fecha = request.date() == null ? LocalDate.now() : request.date();

        Venta venta = ventaRepository.save(Venta.builder()
                .numero(generarNumero())
                .cotizacionId(request.cotizacionId())
                .casoId(request.casoId())
                .clienteId(request.client() == null ? null : request.client().path("id").asLong())
                .clienteJson(JsonUtil.write(request.client()))
                .fecha(fecha)
                .itemsJson(JsonUtil.write(items))
                .subtotal(subtotal)
                .igv(igv)
                .total(total)
                .estado(EstadoVenta.PENDIENTE_COMPRA)
                .creadoPor(authentication == null ? null : authentication.getName())
                .fechaCreacion(LocalDateTime.now())
                .build());
        crearSolicitudes(venta, items);
        return toResponse(venta);
    }

    /** Cambia el estado de la venta; la entrega cierra el caso original. */
    public VentaResponse cambiarEstado(Long id, String estado, Authentication authentication) {
        Venta venta = buscar(id);
        EstadoVenta nuevoEstado = EstadoVenta.from(estado);
        venta.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoVenta.ENTREGADA) {
            venta.setFechaEntrega(LocalDateTime.now());
            casoService.marcarCerrado(venta.getCasoId());
        }
        return toResponse(ventaRepository.save(venta));
    }

    /** Devuelve la entidad de una venta para otros módulos. */
    public Venta obtenerEntidad(Long id) {
        return buscar(id);
    }

    private void crearSolicitudes(Venta venta, List<VentaRequest.ItemRequest> items) {
        for (VentaRequest.ItemRequest item : items) {
            solicitudCompraRepository.save(SolicitudCompra.builder()
                    .ventaId(venta.getId())
                    .ventaNumero(venta.getNumero())
                    .productoId(item.id())
                    .producto(item.name())
                    .cantidad(item.quantity() == null ? 1 : item.quantity())
                    .estado(EstadoSolicitudCompra.PENDIENTE)
                    .fechaCreacion(LocalDateTime.now())
                    .build());
        }
    }

    private JsonNode clienteDe(Cotizacion cotizacion) {
        JsonNode guardado = JsonUtil.read(cotizacion.getClienteJson());
        if (guardado != null && guardado.isObject()) {
            return guardado;
        }
        ObjectNode cliente = objectMapper.createObjectNode();
        cliente.put("id", cotizacion.getClienteId() == null ? 0 : cotizacion.getClienteId());
        cliente.put("name", cotizacion.getCliente() == null ? "Cliente" : cotizacion.getCliente());
        cliente.put("document", guardado == null ? "" : guardado.path("document").asText(""));
        cliente.put("email", guardado == null ? "" : guardado.path("email").asText(""));
        return cliente;
    }

    private BigDecimal subtotalItem(VentaRequest.ItemRequest item) {
        return valor(item.subtotal(), item.price(), item.quantity());
    }

    private BigDecimal valor(BigDecimal subtotal, BigDecimal precio, Integer cantidad) {
        if (subtotal != null) {
            return subtotal;
        }
        if (precio == null) {
            return BigDecimal.ZERO;
        }
        return precio.multiply(BigDecimal.valueOf(cantidad == null ? 0 : cantidad));
    }

    private String generarNumero() {
        long siguiente = ventaRepository.count() + 1;
        String numero;
        do {
            numero = String.format("VEN-%06d", siguiente++);
        } while (ventaRepository.existsByNumero(numero));
        return numero;
    }

    private Venta buscar(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
    }

    private VentaResponse toResponse(Venta venta) {
        List<VentaRequest.ItemRequest> guardados =
                JsonUtil.readList(venta.getItemsJson(), VentaRequest.ItemRequest.class);
        List<VentaResponse.ItemResponse> items = guardados.stream()
                .map(item -> new VentaResponse.ItemResponse(
                        item.id(), item.name(), item.unit(), item.quantity(), item.price(), item.subtotal()))
                .toList();
        return new VentaResponse(
                venta.getId(),
                venta.getNumero(),
                venta.getCotizacionNumero(),
                venta.getCotizacionId(),
                venta.getCasoId(),
                JsonUtil.read(venta.getClienteJson()),
                venta.getFecha() == null ? null : venta.getFecha().toString(),
                items,
                venta.getSubtotal(),
                venta.getIgv(),
                venta.getTotal(),
                venta.getEstado() == null ? null : venta.getEstado().label());
    }
}
