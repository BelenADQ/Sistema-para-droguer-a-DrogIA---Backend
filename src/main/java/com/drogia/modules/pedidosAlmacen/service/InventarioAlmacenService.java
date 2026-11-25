package com.drogia.modules.pedidosAlmacen.service;

import com.drogia.modules.casos.entity.Caso;
import com.drogia.modules.casos.repository.CasoRepository;
import com.drogia.modules.correos.entity.Correo;
import com.drogia.modules.correos.repository.CorreoRepository;
import com.drogia.modules.inventario.entity.InventarioItem;
import com.drogia.modules.inventario.repository.InventarioRepository;
import com.drogia.modules.ordenesCompra.entity.OrdenCompra;
import com.drogia.modules.ordenesCompra.entity.enums.EstadoOrdenCompra;
import com.drogia.modules.ordenesCompra.repository.OrdenCompraRepository;
import com.drogia.modules.pedidosAlmacen.dto.response.InventarioAlmacenResponse;
import com.drogia.modules.pedidosAlmacen.entity.PedidoAlmacen;
import com.drogia.modules.pedidosAlmacen.entity.enums.EstadoPedidoAlmacen;
import com.drogia.modules.pedidosAlmacen.repository.PedidoAlmacenRepository;
import com.drogia.modules.ventas.entity.Venta;
import com.drogia.modules.ventas.repository.VentaRepository;
import com.drogia.shared.util.JsonUtil;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Inventario real de Almacén: calculado en servidor y expuesto como API REST. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioAlmacenService {

    private static final int DIAS_PRIORIDAD = 2;
    private static final int DIAS_POR_DEFECTO = 7;

    private final OrdenCompraRepository ordenRepository;
    private final PedidoAlmacenRepository pedidoRepository;
    private final InventarioRepository inventarioRepository;
    private final VentaRepository ventaRepository;
    private final CasoRepository casoRepository;
    private final CorreoRepository correoRepository;
    private final PedidoAlmacenService pedidoService;
    private final ObjectMapper objectMapper;

    /** Devuelve solo mercadería con stock real mayor a cero. */
    public List<InventarioAlmacenResponse> listarPendientes() {
        List<InventarioItem> lotesActivos = inventarioRepository.findAllByOrderByIdAsc()
                .stream()
                .filter(this::conStock)
                .toList();

        Map<String, List<InventarioItem>> porOrden = lotesActivos.stream()
                .filter(item -> item.getPurchaseOrderCode() != null
                        && !item.getPurchaseOrderCode().isBlank())
                .collect(Collectors.groupingBy(
                        item -> item.getPurchaseOrderCode().trim()));

        List<InventarioAlmacenResponse> resultado = new ArrayList<>();

        for (OrdenCompra orden : ordenRepository.findAllByEstado(EstadoOrdenCompra.CERRADO)) {
            List<InventarioItem> lotes = porOrden.get(orden.getNumero());
            if (lotes == null || lotes.isEmpty()) {
                continue;
            }

            PedidoAlmacen pedido = pedidoRepository
                    .findByOrdenCompraId(orden.getId())
                    .orElse(null);
            if (pedido != null && yaDespachado(pedido)) {
                continue;
            }

            LocalDate fechaLimite = fechaLimite(orden);
            long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaLimite);
            Clasificacion clasificacion = clasificar(diasRestantes);
            JsonNode destino = destinoDe(pedido, orden);

            resultado.add(new InventarioAlmacenResponse(
                    pedido == null ? null : pedido.getId(),
                    orden.getId(),
                    orden.getNumero(),
                    orden.getVentaNumero(),
                    clienteDe(pedido, orden),
                    destino,
                    tipoDespacho(pedido, destino),
                    productosDe(lotes),
                    lotes.size(),
                    lotes.stream().mapToInt(item -> item.getStock() == null ? 0 : item.getStock()).sum(),
                    fechaLimite,
                    diasRestantes,
                    clasificacion.estado(),
                    clasificacion.prioridad(),
                    clasificacion.motivo()));
        }

        resultado.sort(Comparator
                .comparing(InventarioAlmacenResponse::fechaLimiteEntrega)
                .thenComparing(InventarioAlmacenResponse::ordenCompraNumero));
        return resultado;
    }

    private boolean conStock(InventarioItem item) {
        return item.getStock() != null && item.getStock() > 0;
    }

    private boolean yaDespachado(PedidoAlmacen pedido) {
        if (pedido.getDespachoId() != null) {
            return true;
        }
        EstadoPedidoAlmacen estado = pedido.getEstado();
        return estado == EstadoPedidoAlmacen.EN_DESPACHO
                || estado == EstadoPedidoAlmacen.ENTREGADO;
    }

    private LocalDate fechaLimite(OrdenCompra orden) {
        if (orden.getFechaLimiteEntrega() != null) {
            return orden.getFechaLimiteEntrega();
        }
        if (orden.getFecha() != null) {
            return orden.getFecha().plusDays(DIAS_POR_DEFECTO);
        }
        return LocalDate.now().plusDays(DIAS_POR_DEFECTO);
    }

    private Clasificacion clasificar(long dias) {
        if (dias < 0) {
            long atraso = Math.abs(dias);
            return new Clasificacion("Retrasado", "Alta",
                    "Venció hace " + atraso + " día" + (atraso == 1 ? "" : "s"));
        }
        if (dias == 0) {
            return new Clasificacion("Prioridad", "Alta", "Vence hoy");
        }
        if (dias <= DIAS_PRIORIDAD) {
            return new Clasificacion("Prioridad", "Alta",
                    "Vence en " + dias + " día" + (dias == 1 ? "" : "s"));
        }
        return new Clasificacion("Por despachar", "Normal",
                "Vence en " + dias + " días");
    }

    private JsonNode clienteDe(PedidoAlmacen pedido, OrdenCompra orden) {
        JsonNode desdePedido = pedido == null ? null : JsonUtil.read(pedido.getClienteJson());
        if (util(desdePedido)) {
            return desdePedido;
        }

        if (orden.getVentaId() != null) {
            Venta venta = ventaRepository.findById(orden.getVentaId()).orElse(null);
            JsonNode desdeVenta = venta == null ? null : JsonUtil.read(venta.getClienteJson());
            if (util(desdeVenta)) {
                return desdeVenta;
            }
        }

        if (orden.getCasoId() != null) {
            Caso caso = casoRepository.findById(orden.getCasoId()).orElse(null);
            if (caso != null && caso.getCorreoId() != null) {
                Correo correo = correoRepository.findById(caso.getCorreoId()).orElse(null);
                JsonNode extraido = correo == null ? null
                        : JsonUtil.read(correo.getClienteExtraido());
                if (util(extraido)) {
                    return extraido;
                }
                if (correo != null && correo.getRemitente() != null
                        && !correo.getRemitente().isBlank()) {
                    ObjectNode nodo = objectMapper.createObjectNode();
                    nodo.put("name", correo.getRemitente());
                    if (correo.getRemitente().contains("@")) {
                        nodo.put("email", correo.getRemitente());
                    }
                    return nodo;
                }
            }
        }

        ObjectNode nodo = objectMapper.createObjectNode();
        nodo.put("name", "Sin cliente vinculado");
        return nodo;
    }

    private JsonNode destinoDe(PedidoAlmacen pedido, OrdenCompra orden) {
        JsonNode desdePedido = pedido == null ? null : JsonUtil.read(pedido.getDestinoJson());
        if (util(desdePedido)) {
            return desdePedido;
        }
        JsonNode desdeCaso = pedidoService.destinoDe(orden.getCasoId());
        if (util(desdeCaso)) {
            return desdeCaso;
        }
        ObjectNode nodo = objectMapper.createObjectNode();
        nodo.put("direccion", "Sin destino en pedido");
        return nodo;
    }

    private String tipoDespacho(PedidoAlmacen pedido, JsonNode destino) {
        if (pedido != null && pedido.getTipoDespacho() != null) {
            return pedido.getTipoDespacho().name();
        }
        String departamento = destino.path("departamento").asText("");
        String provincia = destino.path("provincia").asText("");
        if ("Arequipa".equalsIgnoreCase(departamento)
                && (provincia.isBlank() || "Arequipa".equalsIgnoreCase(provincia))) {
            return "LOCAL";
        }
        return "COURIER";
    }

    private JsonNode productosDe(List<InventarioItem> lotes) {
        ArrayNode productos = objectMapper.createArrayNode();
        for (InventarioItem item : lotes) {
            ObjectNode producto = productos.addObject();
            producto.put("product", item.getProduct());
            producto.put("quantity", item.getStock() == null ? 0 : item.getStock());
            producto.put("lot", item.getLot());
        }
        return productos;
    }

    private boolean util(JsonNode nodo) {
        return nodo != null && !nodo.isNull() && !nodo.isMissingNode()
                && !(nodo.isObject() && nodo.size() == 0);
    }

    private record Clasificacion(String estado, String prioridad, String motivo) {
    }
}
