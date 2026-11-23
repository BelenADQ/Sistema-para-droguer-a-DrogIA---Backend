package com.drogia.service;

import com.drogia.entities.Correo;
import com.drogia.entities.Documento;
import com.drogia.entities.enums.EstadoCorreo;
import com.drogia.entities.enums.TipoCorreo;
import com.drogia.exception.ApiException;
import com.drogia.repository.CorreoRepository;
import com.drogia.repository.DocumentoRepository;
import com.drogia.service.gemini.GeminiClassification;
import com.drogia.service.gemini.GeminiClassificationService;
import com.drogia.service.storage.DocumentStorageService;
import com.drogia.service.storage.StoredFile;
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
        correoRepository.save(correo);
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
        return correoRepository.save(correo);
    }

    private void aplicarClasificacion(Correo correo, GeminiClassification clasificacion) {
        TipoCorreo tipo = clasificacion.tipoCorreo();
        correo.setTipoClasificacion(tipo == null ? TipoCorreo.OTRO.name() : tipo.name());
        correo.setCodigoCotizacionDetectado(clasificacion.codigoCotizacionDetectado());
        correo.setResumenProductos(clasificacion.resumenProductos());
        correo.setProcesadoAt(LocalDateTime.now());
        correo.setErrorDetalle(null);

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
