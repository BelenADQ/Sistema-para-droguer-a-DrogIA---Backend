package com.drogia.modules.correos.service;

import com.drogia.modules.correos.dto.response.CorreoResponse;
import com.drogia.modules.correos.entity.Correo;
import com.drogia.modules.correos.entity.enums.EstadoCorreo;
import com.drogia.modules.correos.repository.CorreoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.drogia.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Servicio de lectura, persistencia y actualización de correos. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoService {

    private final GmailImapService gmailImapService;
    private final CorreoRepository correoRepository;
    private final ClasificacionCorreoService clasificacionCorreoService;
    private final IngestaProgramadaService ingestaProgramadaService;

    /** Lista las carpetas disponibles en la cuenta de Gmail. */
    public List<String> carpetas() {
        return gmailImapService.listarCarpetas();
    }

    /** Lee correos de Gmail y los guarda en la base de datos. */
    public List<CorreoResponse> listarEnVivo(String carpeta, int limite) {
        return listarEnVivo(carpeta, limite, null);
    }

    /** Lee correos de Gmail y devuelve solo los del área indicada. */
    public List<CorreoResponse> listarEnVivo(String carpeta, int limite, String area) {
        List<GmailImapService.CorreoLeido> leidos =
                gmailImapService.listarCorreos(carpeta, limite);
        List<CorreoResponse> salida = new ArrayList<>();
        for (GmailImapService.CorreoLeido l : leidos) {
            Correo correo = correoRepository.findByMessageId(l.messageId())
                    .orElseGet(() -> Correo.builder()
                            .messageId(l.messageId())
                            .estado(EstadoCorreo.PENDIENTE)
                            .fechaGuardado(LocalDateTime.now())
                            .build());
            boolean esNuevo = correo.getId() == null;
            correo.setCarpeta(l.carpeta());
            correo.setRemitente(l.remitente());
            correo.setAsunto(l.asunto());
            correo.setFecha(l.fecha());
            correo.setSnippet(l.snippet());
            correo.setCuerpoTexto(l.cuerpoTexto());
            correo.setLeido(l.leido());
            salida.add(toResponse(correoRepository.save(correo)));
            log.info(esNuevo
                    ? "Correo nuevo: messageId={} carpeta={}"
                    : "Correo actualizado (conserva estado): messageId={} carpeta={}",
                    l.messageId(), l.carpeta());
        }
        if (area == null || area.isBlank()) {
            return salida;
        }
        return salida.stream()
                .filter(correo -> area.equals(correo.getArea()))
                .toList();
    }

    /** Devuelve todos los correos persistidos (BD), ordenados por fecha descendente. */
    public List<CorreoResponse> historial() {
        return historial(null);
    }

    /** Devuelve los correos del área indicada, o todos cuando no hay filtro. */
    public List<CorreoResponse> historial(String area) {
        var orden = Sort.by(Sort.Direction.DESC, "fecha");
        List<Correo> correos = area == null || area.isBlank()
                ? correoRepository.findAll(orden)
                : correoRepository.findAllByArea(area, orden);
        List<CorreoResponse> historial = correos.stream().map(this::toResponse).toList();
        log.info("Historial consultado: area={} correos={}", area, historial.size());
        return historial;
    }

    /** Actualiza el estado y el área de un correo. */
    public CorreoResponse actualizarEstado(Long id, EstadoCorreo estado, String area) {
        Correo correo = correoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Correo no encontrado: " + id));
        correo.setEstado(estado);
        if (area != null && !area.isBlank()) correo.setArea(area);
        correoRepository.save(correo);
        log.info("PATCH correo id={} estado={} area={}", id, estado, area);
        return toResponse(correo);
    }

    public CorreoResponse clasificar(Long id) {
        return toResponse(clasificacionCorreoService.reclasificar(id));
    }

    public int ejecutarIngesta() {
        return ingestaProgramadaService.ejecutar();
    }

    private CorreoResponse toResponse(Correo c) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return CorreoResponse.builder()
                .id(c.getId())
                .messageId(c.getMessageId())
                .sender(c.getRemitente())
                .subject(c.getAsunto())
                .date(c.getFecha() == null ? null : c.getFecha().format(formatter))
                .area(c.getArea())
                .status(c.getEstado() == null ? null : c.getEstado().label())
                .tipoClasificacion(c.getTipoClasificacion())
                .codigoCotizacionDetectado(c.getCodigoCotizacionDetectado())
                .resumenProductos(c.getResumenProductos())
                .build();
    }
}