package com.drogia.modules.pedidosAlmacen.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.casos.entity.Caso;
import com.drogia.modules.casos.repository.CasoRepository;
import com.drogia.modules.correos.entity.Correo;
import com.drogia.modules.correos.repository.CorreoRepository;
import com.drogia.modules.ordenesCompra.entity.OrdenCompra;
import com.drogia.modules.pedidosAlmacen.dto.request.VerificacionPedidoRequest;
import com.drogia.modules.pedidosAlmacen.dto.response.PedidoAlmacenResponse;
import com.drogia.modules.pedidosAlmacen.entity.PedidoAlmacen;
import com.drogia.modules.pedidosAlmacen.entity.enums.EstadoPedidoAlmacen;
import com.drogia.modules.pedidosAlmacen.entity.enums.TipoDespacho;
import com.drogia.modules.pedidosAlmacen.repository.PedidoAlmacenRepository;
import com.drogia.modules.usuarios.entity.User;
import com.drogia.modules.usuarios.repository.UserRepository;
import com.drogia.modules.ventas.dto.request.VentaRequest;
import com.drogia.modules.ventas.entity.Venta;
import com.drogia.modules.ventas.repository.VentaRepository;
import com.drogia.shared.enums.Rol;
import com.drogia.shared.util.JsonUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de pedidos en almacén con asignación exclusiva por almacenero. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PedidoAlmacenService {

    private final PedidoAlmacenRepository pedidoRepository;
    private final VentaRepository ventaRepository;
    private final CasoRepository casoRepository;
    private final CorreoRepository correoRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** Crea el pedido de almacén cuando la orden de compra llega al almacén. */
    public PedidoAlmacen crearDesdeOrden(OrdenCompra orden) {
        Venta venta = orden.getVentaId() == null ? null
                : ventaRepository.findById(orden.getVentaId()).orElse(null);
        if (venta == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La orden de compra no tiene una venta asociada");
        }
        JsonNode destino = destinoDe(orden.getCasoId());
        TipoDespacho tipo = TipoDespacho.COURIER;
        String departamento = destino.path("departamento").asText("");
        String provincia = destino.path("provincia").asText("");
        if ("Arequipa".equalsIgnoreCase(departamento)
                && (provincia.isBlank() || "Arequipa".equalsIgnoreCase(provincia))) {
            tipo = TipoDespacho.LOCAL;
        }

        PedidoAlmacen pedido = PedidoAlmacen.builder()
                .ventaId(venta.getId())
                .ordenCompraId(orden.getId())
                .casoId(orden.getCasoId())
                .clienteJson(JsonUtil.write(JsonUtil.read(venta.getClienteJson())))
                .productosJson(venta.getItemsJson())
                .destinoJson(JsonUtil.write(destino))
                .tipoDespacho(tipo)
                .estado(EstadoPedidoAlmacen.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();
        PedidoAlmacen guardado = pedidoRepository.save(pedido);
        log.info("Pedido de almacén {} creado para venta {} tipo={}",
                guardado.getId(), venta.getNumero(), tipo);
        return guardado;
    }

    public List<PedidoAlmacenResponse> listar() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion"))
                .stream().map(this::toResponse).toList();
    }

    public PedidoAlmacenResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Un almacenero toma el pedido; los demás reciben 409. */
    public PedidoAlmacenResponse tomar(Long id, Authentication authentication) {
        User usuario = usuarioAlmacen(authentication);
        PedidoAlmacen pedido = buscar(id);
        if (pedido.getAsignadoAId() != null && !pedido.getAsignadoAId().equals(usuario.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "El pedido ya fue tomado por otro almacenero");
        }
        if (pedido.getAsignadoAId() == null) {
            pedido.setAsignadoAId(usuario.getId());
            pedido.setAsignadoANombre(usuario.getNombre());
            pedido.setFechaAsignacion(LocalDateTime.now());
            pedido.setEstado(EstadoPedidoAlmacen.ASIGNADO);
        }
        return toResponse(pedidoRepository.save(pedido));
    }

    /** Registra la verificación de información y lotes. */
    public PedidoAlmacenResponse verificar(
            Long id, VerificacionPedidoRequest request, Authentication authentication) {
        User usuario = usuarioAlmacen(authentication);
        PedidoAlmacen pedido = buscar(id);
        validarAsignado(pedido, usuario);
        pedido.setVerificadoInfo(request.verificadoInfo());
        pedido.setVerificadoLotes(request.verificadoLotes());
        if (request.verificadoInfo() && request.verificadoLotes()) {
            pedido.setEstado(EstadoPedidoAlmacen.VERIFICADO);
        }
        return toResponse(pedidoRepository.save(pedido));
    }

    public PedidoAlmacenResponse cambiarEstado(Long id, String estado, Authentication authentication) {
        User usuario = usuarioAlmacen(authentication);
        PedidoAlmacen pedido = buscar(id);
        validarAsignado(pedido, usuario);
        pedido.setEstado(EstadoPedidoAlmacen.from(estado));
        return toResponse(pedidoRepository.save(pedido));
    }

    /** Vincula el despacho creado con el pedido. */
    public void vincularDespacho(Long pedidoId, Long despachoId) {
        PedidoAlmacen pedido = buscar(pedidoId);
        pedido.setDespachoId(despachoId);
        pedido.setEstado(EstadoPedidoAlmacen.EN_DESPACHO);
        pedidoRepository.save(pedido);
    }

    /** Marca el pedido como entregado al cerrar el despacho. */
    public void marcarEntregado(Long pedidoId) {
        PedidoAlmacen pedido = buscar(pedidoId);
        pedido.setEstado(EstadoPedidoAlmacen.ENTREGADO);
        pedidoRepository.save(pedido);
    }

    public PedidoAlmacen obtenerEntidad(Long id) {
        return buscar(id);
    }

    public JsonNode destinoDe(Long casoId) {
        if (casoId == null) {
            return destinoPorDefecto();
        }
        Caso caso = casoRepository.findById(casoId).orElse(null);
        if (caso == null || caso.getCorreoId() == null) {
            return destinoPorDefecto();
        }
        Correo correo = correoRepository.findById(caso.getCorreoId()).orElse(null);
        JsonNode destino = correo == null ? null : JsonUtil.read(correo.getDestinoExtraido());
        if (destino == null || !destino.isObject()) {
            return destinoPorDefecto();
        }
        return destino;
    }

    private JsonNode destinoPorDefecto() {
        ObjectNode destino = objectMapper.createObjectNode();
        destino.put("departamento", "Arequipa");
        destino.put("provincia", "Arequipa");
        destino.put("direccion", "");
        return destino;
    }

    private void validarAsignado(PedidoAlmacen pedido, User usuario) {
        if (!usuario.getId().equals(pedido.getAsignadoAId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el almacenero asignado puede operar el pedido");
        }
    }

    private User usuarioAlmacen(Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        User usuario = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
        if (usuario.getRol() != Rol.ALMACEN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo Almacenero puede operar pedidos");
        }
        return usuario;
    }

    private PedidoAlmacen buscar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido de almacén no encontrado"));
    }

    private PedidoAlmacenResponse toResponse(PedidoAlmacen pedido) {
        Venta venta = pedido.getVentaId() == null ? null
                : ventaRepository.findById(pedido.getVentaId()).orElse(null);
        return new PedidoAlmacenResponse(
                pedido.getId(), pedido.getVentaId(),
                venta == null ? null : venta.getNumero(),
                pedido.getOrdenCompraId(), pedido.getCasoId(),
                JsonUtil.read(pedido.getClienteJson()),
                JsonUtil.read(pedido.getProductosJson()),
                JsonUtil.read(pedido.getDestinoJson()),
                pedido.getTipoDespacho() == null ? null : pedido.getTipoDespacho().name(),
                pedido.getEstado() == null ? null : pedido.getEstado().label(),
                pedido.getAsignadoAId(), pedido.getAsignadoANombre(),
                pedido.getFechaAsignacion() == null ? null : pedido.getFechaAsignacion().toString(),
                pedido.isVerificadoInfo(), pedido.isVerificadoLotes(), pedido.getDespachoId());
    }
}
