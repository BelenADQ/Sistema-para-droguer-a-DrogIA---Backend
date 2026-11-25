package com.drogia.modules.correos.service;

import com.drogia.modules.casos.service.CasoService;
import com.drogia.modules.correos.entity.Correo;
import com.drogia.modules.correos.entity.Documento;
import com.drogia.modules.correos.entity.enums.EstadoCorreo;
import com.drogia.modules.correos.entity.enums.TipoCorreo;
import com.drogia.modules.correos.entity.enums.TipoRespuesta;
import com.drogia.exception.ApiException;
import com.drogia.modules.correos.repository.CorreoRepository;
import com.drogia.modules.correos.repository.DocumentoRepository;
import com.drogia.modules.correos.service.gemini.GeminiClassification;
import com.drogia.modules.correos.service.gemini.GeminiClassificationService;
import com.drogia.modules.correos.service.storage.DocumentStorageService;
import com.drogia.modules.correos.service.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClasificacionCorreoService {

    private final CorreoRepository correoRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentStorageService storageService;
    private final GeminiClassificationService geminiService;
    private final CasoService casoService;

    @Transactional
    public boolean procesar(GmailImapService.CorreoLeido leido) {
        Correo correo = correoRepository.findByMessageId(leido.messageId())
                .orElseGet(() -> Correo.builder()
                        .messageId(leido.messageId())
                        .estado(EstadoCorreo.PENDIENTE)
                        .fechaGuardado(LocalDateTime.now())
                        .build());
        correo.setCarpeta(leido.carpeta());
        correo.setRemitente(leido.remitente());
        correo.setAsunto(leido.asunto());
        correo.setFecha(leido.fecha());
        correo.setSnippet(leido.snippet());
        correo.setCuerpoTexto(leido.cuerpoTexto());
        correo.setLeido(leido.leido());
        correo.setErrorDetalle(null);
        correo = correoRepository.save(correo);

        List<GeminiClassificationService.Attachment> adjuntosParaGemini = new ArrayList<>();
        if (leido.adjuntos() != null) {
            for (GmailImapService.AdjuntoLeido adjunto : leido.adjuntos()) {
                StoredFile almacenado = storageService.storeBytes(
                        adjunto.filename(), adjunto.mimeType(), adjunto.bytes(),
                        "correos/" + sanitize(leido.messageId()));
                documentoRepository.save(Documento.builder()
                        .correo(correo)
                        .nombreOriginal(adjunto.filename())
                        .storagePath(almacenado.storagePath())
                        .mimeType(adjunto.mimeType())
                        .tamanoBytes(almacenado.size())
                        .sha256(almacenado.sha256())
                        .build());
                Path ruta = storageService.resolve(almacenado.storagePath());
                adjuntosParaGemini.add(new GeminiClassificationService.Attachment(
                        adjunto.filename(), adjunto.mimeType(), ruta));
            }
        }

        GeminiClassification clasificacion = geminiService.classify(
                leido.asunto(), leido.cuerpoTexto(), leido.remitente(), adjuntosParaGemini);
        aplicarClasificacion(correo, clasificacion);
        correo.setClasificaciones((correo.getClasificaciones() == null ? 0 : correo.getClasificaciones()) + 1);
        correo = correoRepository.save(correo);
        casoService.sincronizar(correo);
        log.info("Correo clasificado messageId={} tipo={} estado={}",
                leido.messageId(), correo.getTipoClasificacion(), correo.getEstado());
        return true;
    }

    @Transactional
    public void marcarError(GmailImapService.CorreoLeido leido, String detalle) {
        Correo correo = correoRepository.findByMessageId(leido.messageId())
                .orElseGet(() -> Correo.builder()
                        .messageId(leido.messageId())
                        .estado(EstadoCorreo.PENDIENTE)
                        .fechaGuardado(LocalDateTime.now())
                        .build());
        correo.setCarpeta(leido.carpeta());
        correo.setRemitente(leido.remitente());
        correo.setAsunto(leido.asunto());
        correo.setFecha(leido.fecha());
        correo.setSnippet(leido.snippet());
        correo.setCuerpoTexto(leido.cuerpoTexto());
        correo.setEstado(EstadoCorreo.REVISION);
        correo.setErrorDetalle(detalle != null && detalle.length() > 2000 ? detalle.substring(0, 2000) : detalle);
        correoRepository.save(correo);
    }

    @Transactional
    public Correo reclasificar(Long id) {
        Correo correo = correoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Correo no encontrado"));
        List<GeminiClassificationService.Attachment> adjuntos = documentoRepository.findByCorreoIdOrderByIdAsc(id).stream()
                .map(documento -> new GeminiClassificationService.Attachment(
                        documento.getNombreOriginal(),
                        documento.getMimeType(),
                        storageService.resolve(documento.getStoragePath())))
                .toList();
        GeminiClassification clasificacion = geminiService.classify(
                correo.getAsunto(), correo.getCuerpoTexto(), correo.getRemitente(), adjuntos);
        aplicarClasificacion(correo, clasificacion);
        correo.setClasificaciones((correo.getClasificaciones() == null ? 0 : correo.getClasificaciones()) + 1);
        correo = correoRepository.save(correo);
        casoService.sincronizar(correo);
        return correo;
    }

    private void aplicarClasificacion(Correo correo, GeminiClassification clasificacion) {
        TipoCorreo tipo = clasificacion.tipoCorreo();
        correo.setTipoClasificacion(tipo == null ? TipoCorreo.OTRO.name() : tipo.name());
        correo.setCodigoCotizacionDetectado(clasificacion.codigoCotizacionDetectado());
        correo.setResumenProductos(clasificacion.resumenProductos());
        correo.setTipoRespuesta(clasificacion.tipoRespuesta() == null
                ? TipoRespuesta.OTRO.name() : clasificacion.tipoRespuesta().name());
        correo.setCodigoReferencia(clasificacion.codigoReferencia());
        correo.setClienteExtraido(clasificacion.clienteJson());
        correo.setProductosExtraidos(clasificacion.productosJson());
        correo.setDestinoExtraido(clasificacion.destinoJson());
        correo.setProcesadoAt(LocalDateTime.now());
        correo.setErrorDetalle(null);

        if (clasificacion.tipoRespuesta() == TipoRespuesta.NUEVA_SOLICITUD) {
            correo.setEstado(EstadoCorreo.ANALIZADO);
            correo.setArea("Ventas");
            return;
        }
        if (clasificacion.tipoRespuesta() == TipoRespuesta.ORDEN_COMPRA) {
            correo.setArea("Compras");
            if (clasificacion.codigoReferencia() == null
                    && clasificacion.codigoCotizacionDetectado() == null) {
                correo.setEstado(EstadoCorreo.REVISION);
                correo.setErrorDetalle("Orden de compra sin código de referencia detectado");
            } else {
                correo.setEstado(EstadoCorreo.ANALIZADO);
            }
            return;
        }

        if (tipo == TipoCorreo.SOLICITUD_COTIZACION) {
            correo.setEstado(EstadoCorreo.ANALIZADO);
            correo.setArea("Ventas");
            return;
        }
        if (tipo == TipoCorreo.ORDEN_COMPRA) {
            correo.setArea("Compras");
            if (clasificacion.codigoCotizacionDetectado() == null) {
                correo.setEstado(EstadoCorreo.REVISION);
                correo.setErrorDetalle("Orden de compra sin código COT detectado");
            } else {
                correo.setEstado(EstadoCorreo.ANALIZADO);
            }
            return;
        }
        correo.setEstado(EstadoCorreo.REVISION);
        correo.setArea(null);
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
