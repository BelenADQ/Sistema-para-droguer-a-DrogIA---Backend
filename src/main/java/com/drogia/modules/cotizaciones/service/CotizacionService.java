package com.drogia.modules.cotizaciones.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.casos.entity.Caso;
import com.drogia.modules.casos.service.CasoService;
import com.drogia.modules.clientes.entity.Cliente;
import com.drogia.modules.clientes.repository.ClienteRepository;
import com.drogia.modules.correos.entity.Correo;
import com.drogia.modules.correos.repository.CorreoRepository;
import com.drogia.modules.cotizaciones.dto.request.CotizacionRequest;
import com.drogia.modules.cotizaciones.dto.request.EstadoCotizacionRequest;
import com.drogia.modules.cotizaciones.dto.response.CotizacionResponse;
import com.drogia.modules.cotizaciones.entity.Cotizacion;
import com.drogia.modules.cotizaciones.entity.enums.EstadoCotizacion;
import com.drogia.modules.cotizaciones.repository.CotizacionRepository;
import com.drogia.modules.usuarios.entity.User;
import com.drogia.modules.usuarios.repository.UserRepository;
import com.drogia.security.RolResolver;
import com.drogia.shared.enums.Rol;
import com.drogia.shared.util.JsonUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de cotizaciones creadas por Ventas desde casos de correo. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CotizacionService {

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    private final CotizacionRepository cotizacionRepository;
    private final CasoService casoService;
    private final CorreoRepository correoRepository;
    private final ClienteRepository clienteRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** Lista las cotizaciones visibles para el rol autenticado. */
    public List<CotizacionResponse> listar(Authentication authentication) {
        Sort orden = Sort.by(Sort.Direction.DESC, "fechaCreacion");
        Rol rol = RolResolver.from(authentication);
        List<Cotizacion> cotizaciones;
        if (rol == Rol.ADMINISTRACION) {
            cotizaciones = cotizacionRepository.findAll(orden);
        } else if (rol == Rol.VENTAS) {
            User usuario = usuarioActual(authentication);
            cotizaciones = cotizacionRepository.findAllByCreadoPorId(usuario.getId(), orden);
        } else {
            cotizaciones = List.of();
        }
        return cotizaciones.stream().map(this::toResponse).toList();
    }

    /** Devuelve una cotización permitida según el rol. */
    public CotizacionResponse obtener(Long id, Authentication authentication) {
        Cotizacion cotizacion = buscar(id);
        validarLectura(cotizacion, authentication);
        return toResponse(cotizacion);
    }

    /** Crea una cotización. Solo el usuario de Ventas que tomó el caso puede hacerlo. */
    public CotizacionResponse crear(CotizacionRequest request, Authentication authentication) {
        Rol rol = RolResolver.from(authentication);
        if (rol != Rol.VENTAS) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo Ventas puede crear cotizaciones");
        }
        if (request.casoId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El caso es obligatorio");
        }

        User usuario = usuarioActual(authentication);
        Caso caso = casoService.obtenerEntidad(request.casoId());
        if (!"Ventas".equals(caso.getArea())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El caso no pertenece al área de Ventas");
        }
        if (!usuario.getId().equals(caso.getAsignadoAId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El caso debe ser tomado por el usuario de Ventas");
        }
        if (cotizacionRepository.findByCasoId(caso.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El caso ya tiene una cotización");
        }

        List<CotizacionRequest.ItemRequest> items =
                request.items() == null ? List.of() : request.items();
        BigDecimal subtotal = request.subtotal() != null
                ? request.subtotal()
                : items.stream().map(this::subtotalItem).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = request.igv() != null
                ? request.igv()
                : subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);

        JsonNode cliente = construirCliente(request);
        JsonNode destino = request.destino() != null
                ? request.destino()
                : destinoDelCorreo(caso.getCorreoId());

        Cotizacion cotizacion = Cotizacion.builder()
                .numero(generarNumero())
                .casoId(caso.getId())
                .correoId(caso.getCorreoId())
                .creadoPorId(usuario.getId())
                .creadoPorNombre(usuario.getNombre())
                .clienteId(cliente.path("id").asLong())
                .cliente(cliente.path("name").asText(null))
                .clienteJson(JsonUtil.write(cliente))
                .destinoJson(JsonUtil.write(destino))
                .proveedoresJson(JsonUtil.write(request.proveedores() == null
                        ? objectMapper.createArrayNode() : request.proveedores()))
                .codigoSolicitud(caso.getCodigoSolicitud())
                .estado(EstadoCotizacion.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .fechaLimiteCotizacion(caso.getFechaLimiteCotizacion())
                .condicionesPago(request.condicionesPago())
                .vigencia(request.vigencia() == null ? "7 días" : request.vigencia())
                .subtotal(subtotal)
                .igv(igv)
                .total(subtotal.add(igv))
                .observaciones(request.observaciones())
                .itemsJson(JsonUtil.write(items))
                .build();
        cotizacion = cotizacionRepository.save(cotizacion);

        casoService.vincularCotizacion(caso.getId(), cotizacion.getId());
        log.info("Cotización {} creada para caso {} por usuario {}",
                cotizacion.getNumero(), caso.getId(), usuario.getUsername());
        return toResponse(cotizacion);
    }

    /** Actualiza una cotización. Solo el usuario de Ventas que la creó puede hacerlo. */
    public CotizacionResponse actualizar(
            Long id, CotizacionRequest request, Authentication authentication) {
        Cotizacion cotizacion = buscar(id);
        validarEscritura(cotizacion, authentication);
        if (cotizacion.getEstado() == EstadoCotizacion.CONVERTIDA) {
            throw new ApiException(HttpStatus.CONFLICT, "La cotización ya fue convertida en venta");
        }

        List<CotizacionRequest.ItemRequest> items =
                request.items() == null ? List.of() : request.items();
        BigDecimal subtotal = request.subtotal() != null
                ? request.subtotal()
                : items.stream().map(this::subtotalItem).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = request.igv() != null
                ? request.igv()
                : subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
        JsonNode cliente = construirCliente(request);

        cotizacion.setClienteId(cliente.path("id").asLong());
        cotizacion.setCliente(cliente.path("name").asText(null));
        cotizacion.setClienteJson(JsonUtil.write(cliente));
        if (request.destino() != null) {
            cotizacion.setDestinoJson(JsonUtil.write(request.destino()));
        }
        if (request.proveedores() != null) {
            cotizacion.setProveedoresJson(JsonUtil.write(request.proveedores()));
        }
        cotizacion.setCondicionesPago(request.condicionesPago());
        cotizacion.setVigencia(request.vigencia() == null ? "7 días" : request.vigencia());
        cotizacion.setSubtotal(subtotal);
        cotizacion.setIgv(igv);
        cotizacion.setTotal(subtotal.add(igv));
        cotizacion.setItemsJson(JsonUtil.write(items));
        cotizacion.setObservaciones(request.observaciones());
        return toResponse(cotizacionRepository.save(cotizacion));
    }

    /** Guarda las cotizaciones de proveedores usadas como soporte por Ventas. */
    public JsonNode guardarProveedores(
            Long id, JsonNode proveedores, Authentication authentication) {
        Cotizacion cotizacion = buscar(id);
        validarEscritura(cotizacion, authentication);
        cotizacion.setProveedoresJson(JsonUtil.write(proveedores == null
                ? objectMapper.createArrayNode() : proveedores));
        cotizacionRepository.save(cotizacion);
        return proveedores == null ? objectMapper.createArrayNode() : proveedores;
    }

    public JsonNode listarProveedores(Long id, Authentication authentication) {
        Cotizacion cotizacion = buscar(id);
        validarLectura(cotizacion, authentication);
        JsonNode proveedores = JsonUtil.read(cotizacion.getProveedoresJson());
        return proveedores == null ? objectMapper.createArrayNode() : proveedores;
    }

    /** Cambia el estado de una cotización. Solo Ventas puede hacerlo. */
    public CotizacionResponse cambiarEstado(
            Long id, EstadoCotizacionRequest request, Authentication authentication) {
        if (RolResolver.from(authentication) != Rol.VENTAS) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo Ventas puede cambiar el estado");
        }
        Cotizacion cotizacion = buscar(id);
        validarEscritura(cotizacion, authentication);
        cotizacion.setEstado(request.estado());
        if (request.estado() == EstadoCotizacion.ACEPTADA && cotizacion.getCasoId() != null) {
            casoService.marcarAceptado(cotizacion.getCasoId());
        }
        if (request.estado() == EstadoCotizacion.CONVERTIDA && cotizacion.getCasoId() != null) {
            casoService.marcarConvertido(cotizacion.getCasoId());
        }
        return toResponse(cotizacionRepository.save(cotizacion));
    }

    /** Registra el envío de la cotización al cliente. */
    public CotizacionResponse marcarEnviada(Long id, Authentication authentication) {
        Cotizacion cotizacion = buscar(id);
        validarEscritura(cotizacion, authentication);
        cotizacion.setFechaEnvio(LocalDateTime.now());
        if (cotizacion.getEstado() == EstadoCotizacion.PENDIENTE) {
            cotizacion.setEstado(EstadoCotizacion.ENVIADA);
        }
        return toResponse(cotizacionRepository.save(cotizacion));
    }

    private JsonNode construirCliente(CotizacionRequest request) {
        Cliente cliente = request.clienteId() == null
                ? null
                : clienteRepository.findById(request.clienteId()).orElse(null);
        ObjectNode nodo = objectMapper.createObjectNode();
        nodo.put("id", cliente != null ? cliente.getId()
                : (request.clienteId() == null ? 0 : request.clienteId()));
        nodo.put("name", cliente != null ? cliente.getName()
                : (request.cliente() == null ? "Cliente" : request.cliente()));
        nodo.put("document", cliente != null && cliente.getDocument() != null
                ? cliente.getDocument()
                : (request.clienteDocumento() == null ? "" : request.clienteDocumento()));
        String email = cliente != null && cliente.getEmail() != null
                ? cliente.getEmail()
                : request.clienteEmail();
        if (email == null && request.cliente() != null && request.cliente().contains("@")) {
            email = request.cliente();
        }
        nodo.put("email", email == null ? "" : email);
        return nodo;
    }

    private JsonNode destinoDelCorreo(Long correoId) {
        if (correoId == null) {
            return objectMapper.createObjectNode();
        }
        Correo correo = correoRepository.findById(correoId).orElse(null);
        JsonNode destino = correo == null ? null : JsonUtil.read(correo.getDestinoExtraido());
        return destino == null ? objectMapper.createObjectNode() : destino;
    }

    private BigDecimal subtotalItem(CotizacionRequest.ItemRequest item) {
        if (item.subtotal() != null) {
            return item.subtotal();
        }
        if (item.precioUnitario() == null) {
            return BigDecimal.ZERO;
        }
        return item.precioUnitario().multiply(
                BigDecimal.valueOf(item.cantidad() == null ? 0 : item.cantidad()));
    }

    private Cotizacion buscar(Long id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cotización no encontrada"));
    }

    private void validarLectura(Cotizacion cotizacion, Authentication authentication) {
        Rol rol = RolResolver.from(authentication);
        if (rol == Rol.ADMINISTRACION) {
            return;
        }
        User usuario = usuarioActual(authentication);
        if (rol != Rol.VENTAS || !usuario.getId().equals(cotizacion.getCreadoPorId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Acceso denegado a la cotización");
        }
    }

    private void validarEscritura(Cotizacion cotizacion, Authentication authentication) {
        if (RolResolver.from(authentication) != Rol.VENTAS) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo Ventas puede modificar cotizaciones");
        }
        User usuario = usuarioActual(authentication);
        if (!usuario.getId().equals(cotizacion.getCreadoPorId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Solo el responsable de la cotización puede modificarla");
        }
    }

    private String generarNumero() {
        long siguiente = cotizacionRepository.count() + 1;
        String numero;
        do {
            numero = String.format("COT-%06d", siguiente++);
        } while (cotizacionRepository.existsByNumero(numero));
        return numero;
    }

    private User usuarioActual(Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        return userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    private List<CotizacionRequest.ItemRequest> leerItems(String itemsJson) {
        return JsonUtil.readList(itemsJson, CotizacionRequest.ItemRequest.class);
    }

    private CotizacionResponse toResponse(Cotizacion cotizacion) {
        Caso caso = cotizacion.getCasoId() == null
                ? null
                : casoService.obtenerEntidad(cotizacion.getCasoId());
        Correo correo = cotizacion.getCorreoId() == null
                ? null
                : correoRepository.findById(cotizacion.getCorreoId()).orElse(null);
        List<CotizacionRequest.ItemRequest> guardados = leerItems(cotizacion.getItemsJson());

        List<CotizacionResponse.ItemResponse> items = guardados.stream()
                .map(item -> new CotizacionResponse.ItemResponse(
                        item.productoId(), item.producto(), item.unidad(), item.cantidad(),
                        item.precioUnitario(), item.subtotal()))
                .toList();
        List<CotizacionResponse.ProductoResponse> products = new ArrayList<>();
        for (int i = 0; i < guardados.size(); i++) {
            CotizacionRequest.ItemRequest item = guardados.get(i);
            products.add(new CotizacionResponse.ProductoResponse(
                    item.productoId(), item.producto(), item.unidad(), item.cantidad(),
                    item.precioUnitario(), item.subtotal(), i + 1));
        }
        JsonNode client = JsonUtil.read(cotizacion.getClienteJson());
        CotizacionResponse.ClienteResponse clientDto = client == null
                ? new CotizacionResponse.ClienteResponse(
                        cotizacion.getClienteId() == null ? 0 : cotizacion.getClienteId(),
                        cotizacion.getCliente() == null ? "" : cotizacion.getCliente(), "", "")
                : new CotizacionResponse.ClienteResponse(
                        client.path("id").asLong(), client.path("name").asText(""),
                        client.path("document").asText(""), client.path("email").asText(""));
        JsonNode destino = JsonUtil.read(cotizacion.getDestinoJson());
        JsonNode proveedores = JsonUtil.read(cotizacion.getProveedoresJson());

        return new CotizacionResponse(
                cotizacion.getId(),
                cotizacion.getNumero(),
                cotizacion.getNumero(),
                cotizacion.getCasoId(),
                cotizacion.getCorreoId(),
                cotizacion.getCreadoPorId(),
                cotizacion.getCreadoPorNombre(),
                caso == null ? null : caso.getAsignadoANombre(),
                cotizacion.getCliente(),
                clientDto,
                cotizacion.getEstado() == null ? null : cotizacion.getEstado().label(),
                cotizacion.getEstado() == null ? null : cotizacion.getEstado().label(),
                cotizacion.getFechaCreacion(),
                cotizacion.getFechaCreacion() == null ? null
                        : cotizacion.getFechaCreacion().toLocalDate().toString(),
                cotizacion.getCondicionesPago(),
                cotizacion.getVigencia(),
                cotizacion.getSubtotal(),
                cotizacion.getIgv(),
                cotizacion.getTotal(),
                cotizacion.getObservaciones(),
                items,
                products,
                destino == null ? objectMapper.createObjectNode() : destino,
                proveedores == null ? objectMapper.createArrayNode() : proveedores,
                cotizacion.getCodigoSolicitud(),
                cotizacion.getFechaLimiteCotizacion(),
                cotizacion.getFechaEnvio(),
                correo == null ? null : correo.getAsunto(),
                correo == null ? null : correo.getRemitente(),
                caso == null ? null : caso.getArea());
    }
}
