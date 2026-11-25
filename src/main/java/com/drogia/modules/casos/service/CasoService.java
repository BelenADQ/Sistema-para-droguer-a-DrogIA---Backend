package com.drogia.modules.casos.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.casos.dto.request.EstadoCasoRequest;
import com.drogia.modules.casos.dto.response.CasoResponse;
import com.drogia.modules.casos.entity.Caso;
import com.drogia.modules.casos.entity.enums.EstadoCaso;
import com.drogia.modules.casos.repository.CasoRepository;
import com.drogia.modules.correos.entity.Correo;
import com.drogia.modules.correos.repository.CorreoRepository;
import com.drogia.modules.usuarios.entity.User;
import com.drogia.modules.usuarios.repository.UserRepository;
import com.drogia.security.RolResolver;
import com.drogia.shared.enums.Rol;
import com.drogia.shared.util.JsonUtil;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de casos generados desde correos clasificados. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CasoService {

    private final CasoRepository casoRepository;
    private final CorreoRepository correoRepository;
    private final UserRepository userRepository;

    /** Crea o actualiza el caso asociado a un correo ya clasificado. */
    public void sincronizar(@NotNull Correo correo) {
        if (correo.getId() == null) {
            return;
        }
        Caso caso = casoRepository.findByCorreoId(correo.getId()).orElse(null);
        if (caso == null) {
            casoRepository.save(Caso.builder()
                    .correoId(correo.getId())
                    .area(correo.getArea())
                    .estado(EstadoCaso.PENDIENTE)
                    .fechaCreacion(LocalDateTime.now())
                    .build());
            log.info("Caso creado desde correo id={} area={}", correo.getId(), correo.getArea());
            return;
        }
        if (!Objects.equals(caso.getArea(), correo.getArea())) {
            caso.setArea(correo.getArea());
            casoRepository.save(caso);
            log.info("Caso id={} reclasificado a area={}", caso.getId(), caso.getArea());
        }
    }

    /** Lista los casos permitidos según el rol del usuario. */
    public List<CasoResponse> listar(Authentication authentication) {
        Sort orden = Sort.by(Sort.Direction.DESC, "fechaCreacion");
        Rol rol = RolResolver.from(authentication);
        List<Caso> casos;
        if (rol == Rol.ADMINISTRACION) {
            casos = casoRepository.findAll(orden);
        } else if (rol == Rol.VENTAS) {
            casos = casoRepository.findAllByArea("Ventas", orden);
        } else if (rol == Rol.COMPRAS) {
            casos = casoRepository.findAllByArea("Compras", orden);
        } else {
            casos = List.of();
        }
        return casos.stream().map(this::toResponse).toList();
    }

    /** Devuelve un caso permitido según el rol del usuario. */
    public CasoResponse obtener(Long id, Authentication authentication) {
        Caso caso = buscar(id);
        if (!puedeVer(caso, RolResolver.from(authentication))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Acceso denegado al caso");
        }
        return toResponse(caso);
    }

    /** Devuelve la entidad de un caso para uso de otros módulos. */
    public Caso obtenerEntidad(Long id) {
        return buscar(id);
    }

    /** Permite que un usuario de Ventas o Compras tome el caso de su área. */
    public CasoResponse tomar(Long id, Authentication authentication) {
        Rol rol = RolResolver.from(authentication);
        String areaRol = areaPorRolDelSegmento(rol);
        if (areaRol == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Su rol no puede tomar casos");
        }

        Caso caso = buscar(id);
        if (!areaRol.equals(caso.getArea())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El caso no pertenece a su área");
        }

        User usuario = usuarioActual(authentication);
        if (caso.getAsignadoAId() != null && !caso.getAsignadoAId().equals(usuario.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "El caso ya fue tomado por otro usuario");
        }
        if (caso.getAsignadoAId() != null) {
            log.info("Caso id={} ya estaba tomado por usuario={}; no se modifica el estado",
                    id, usuario.getUsername());
            return toResponse(caso);
        }

        caso.setAsignadoAId(usuario.getId());
        caso.setAsignadoANombre(usuario.getNombre());
        caso.setFechaAsignacion(LocalDateTime.now());
        caso.setEstado(EstadoCaso.ASIGNADO);
        caso.setFechaCierre(null);
        if ("Ventas".equals(caso.getArea()) && caso.getCodigoSolicitud() == null) {
            caso.setCodigoSolicitud(generarCodigoSolicitud());
            caso.setFechaLimiteCotizacion(LocalDateTime.now().plusDays(7));
        }
        Caso guardado = casoRepository.save(caso);
        log.info("Caso id={} tomado por usuario={} rol={}", id, usuario.getUsername(), rol);
        return toResponse(guardado);
    }

    /** Actualiza manualmente el estado de un caso por el usuario asignado. */
    public CasoResponse actualizarEstado(Long id, EstadoCasoRequest request, Authentication authentication) {
        Rol rol = RolResolver.from(authentication);
        if (rol != Rol.VENTAS && rol != Rol.COMPRAS) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Su rol no puede actualizar el caso");
        }

        Caso caso = buscar(id);
        User usuario = usuarioActual(authentication);
        if (!usuario.getId().equals(caso.getAsignadoAId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el usuario asignado puede actualizar el caso");
        }

        caso.setEstado(request.estado());
        if (request.observaciones() != null && !request.observaciones().isBlank()) {
            caso.setObservaciones(request.observaciones().trim());
        }
        if (request.estado() == EstadoCaso.CERRADO || request.estado() == EstadoCaso.CONVERTIDO) {
            caso.setFechaCierre(LocalDateTime.now());
        } else {
            caso.setFechaCierre(null);
        }
        return toResponse(casoRepository.save(caso));
    }

    /** Vincula la cotización creada y marca el caso como cotizado. */
    public void vincularCotizacion(Long casoId, Long cotizacionId) {
        if (casoId == null) {
            return;
        }
        Caso caso = buscar(casoId);
        caso.setCotizacionId(cotizacionId);
        caso.setEstado(EstadoCaso.COTIZADO);
        casoRepository.save(caso);
    }

    /** Marca el caso como aceptado cuando se acepta su cotización. */
    public void marcarAceptado(Long casoId) {
        if (casoId == null) {
            return;
        }
        Caso caso = buscar(casoId);
        caso.setEstado(EstadoCaso.ACEPTADO);
        casoRepository.save(caso);
    }

    /** Marca el caso como cerrado al completar la entrega. */
    public void marcarCerrado(Long casoId) {
        if (casoId == null) {
            return;
        }
        Caso caso = buscar(casoId);
        caso.setEstado(EstadoCaso.CERRADO);
        caso.setFechaCierre(LocalDateTime.now());
        casoRepository.save(caso);
    }

    /** Vincula la venta derivada del caso. */
    public void vincularVenta(Long casoId, Long ventaId) {
        if (casoId == null) {
            return;
        }
        Caso caso = buscar(casoId);
        caso.setVentaId(ventaId);
        casoRepository.save(caso);
    }

    /** Vincula la orden de compra derivada del caso. */
    public void vincularOrdenCompra(Long casoId, Long ordenCompraId) {
        if (casoId == null) {
            return;
        }
        Caso caso = buscar(casoId);
        caso.setOrdenCompraId(ordenCompraId);
        casoRepository.save(caso);
    }

    /** Vincula el pedido de almacén derivado del caso. */
    public void vincularPedidoAlmacen(Long casoId, Long pedidoAlmacenId) {
        if (casoId == null) {
            return;
        }
        Caso caso = buscar(casoId);
        caso.setPedidoAlmacenId(pedidoAlmacenId);
        casoRepository.save(caso);
    }

    /** Sincroniza el caso de un correo tras un cambio manual de estado o área. */
    public void sincronizarEstadoCorreo(Correo correo) {
        if (correo == null || correo.getId() == null) {
            return;
        }
        sincronizar(correo);
        Caso caso = casoRepository.findByCorreoId(correo.getId()).orElse(null);
        if (caso != null && !caso.getId().equals(correo.getCasoId())) {
            correo.setCasoId(caso.getId());
            correoRepository.save(correo);
        }
    }

    /** Sincroniza el identificador de caso en el correo de origen. */
    public void sincronizarEstadoCorreo(Long correoId, Long casoId) {
        if (correoId == null || casoId == null) {
            return;
        }
        Correo correo = correoRepository.findById(correoId).orElse(null);
        if (correo == null || casoId.equals(correo.getCasoId())) {
            return;
        }
        correo.setCasoId(casoId);
        correoRepository.save(correo);
    }

    private String generarCodigoSolicitud() {
        String codigo;
        long siguiente = casoRepository.count() + 1;
        do {
            codigo = String.format("SOL-%d-%06d", java.time.LocalDate.now().getYear(), siguiente++);
        } while (casoRepository.existsByCodigoSolicitud(codigo));
        return codigo;
    }

    /** Marca el caso como convertido cuando su cotización pasa a venta. */
    public void marcarConvertido(Long casoId) {
        if (casoId == null) {
            return;
        }
        Caso caso = buscar(casoId);
        caso.setEstado(EstadoCaso.CONVERTIDO);
        caso.setFechaCierre(LocalDateTime.now());
        casoRepository.save(caso);
    }

    private Caso buscar(Long id) {
        return casoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caso no encontrado"));
    }

    private boolean puedeVer(Caso caso, Rol rol) {
        if (rol == Rol.ADMINISTRACION) {
            return true;
        }
        if (rol == Rol.VENTAS) {
            return "Ventas".equals(caso.getArea());
        }
        if (rol == Rol.COMPRAS) {
            return "Compras".equals(caso.getArea());
        }
        return false;
    }

    private String areaPorRolDelSegmento(Rol rol) {
        if (rol == Rol.VENTAS) {
            return "Ventas";
        }
        if (rol == Rol.COMPRAS) {
            return "Compras";
        }
        return null;
    }

    private User usuarioActual(Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        return userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    private CasoResponse toResponse(Caso caso) {
        Correo correo = caso.getCorreoId() == null
                ? null
                : correoRepository.findById(caso.getCorreoId()).orElse(null);
        return new CasoResponse(
                caso.getId(),
                caso.getCorreoId(),
                correo == null ? null : correo.getRemitente(),
                correo == null ? null : correo.getAsunto(),
                caso.getArea(),
                caso.getEstado() == null ? null : caso.getEstado().label(),
                caso.getEstado() == null ? null : caso.getEstado().label(),
                caso.getAsignadoAId(),
                caso.getAsignadoANombre(),
                caso.getFechaAsignacion(),
                caso.getCotizacionId(),
                caso.getCodigoSolicitud(),
                caso.getFechaLimiteCotizacion(),
                correo == null ? null : JsonUtil.read(correo.getClienteExtraido()),
                correo == null ? null : JsonUtil.read(correo.getProductosExtraidos()),
                correo == null ? null : JsonUtil.read(correo.getDestinoExtraido()),
                caso.getVentaId(),
                caso.getOrdenCompraId(),
                caso.getPedidoAlmacenId(),
                caso.getFechaCreacion(),
                caso.getFechaCierre(),
                caso.getObservaciones(),
                correo == null ? null : correo.getCodigoCotizacionDetectado(),
                correo == null ? null : correo.getResumenProductos());
    }
}
