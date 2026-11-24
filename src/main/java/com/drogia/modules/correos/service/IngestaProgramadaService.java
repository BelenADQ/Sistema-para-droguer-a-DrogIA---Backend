package com.drogia.modules.correos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestaProgramadaService {

    private final GmailImapService gmailImapService;
    private final ClasificacionCorreoService clasificacionCorreoService;

    @Value("${gmail.ingesta.enabled:false}")
    private boolean enabled;

    @Value("${gmail.ingesta.batch-size:50}")
    private int batchSize;

    @Value("${gmail.ingesta.folder:INBOX}")
    private String folder;

    @Scheduled(fixedDelayString = "${gmail.ingesta.fixed-delay-ms:60000}")
    public void ejecutarProgramado() {
        if (!enabled) {
            return;
        }
        try {
            ejecutar();
        } catch (Exception e) {
            log.error("Error en ingesta programada: {}", e.getClass().getSimpleName());
        }
    }

    public int ejecutar() {
        return gmailImapService.procesarNuevos(folder, batchSize, leido -> {
            try {
                return clasificacionCorreoService.procesar(leido);
            } catch (Exception e) {
                log.error("No se pudo clasificar messageId={}: {}", leido.messageId(), e.getClass().getSimpleName());
                clasificacionCorreoService.marcarError(leido, e.getMessage());
                return false;
            }
        });
    }
}
