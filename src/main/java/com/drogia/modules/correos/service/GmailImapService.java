package com.drogia.modules.correos.service;

import com.drogia.exception.GmailException;
import jakarta.mail.*;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
/** Servicio de conexión y lectura de correos de Gmail por IMAP. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailImapService {

    @Value("${gmail.imap.host}") private String host;
    @Value("${gmail.imap.port}") private int port;
    @Value("${gmail.user}") private String user;
    @Value("${gmail.app-password}") private String appPassword;

    public record AdjuntoLeido(String filename, String mimeType, byte[] bytes) {}

    /** Datos mínimos extraídos de cada mensaje leído (antes de persistir). */
    public record CorreoLeido(String messageId, String carpeta, String remitente,
                              String asunto, LocalDateTime fecha, String snippet,
                              boolean leido, String cuerpoTexto,
                              List<AdjuntoLeido> adjuntos) {}

    /** Abre la conexión IMAPS con la app password y la devuelve lista. */
    private Store conectar() {
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", host);
            props.put("mail.imaps.port", String.valueOf(port));
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.connectiontimeout", "10000");
            props.put("mail.imaps.timeout", "20000");
            Store store = Session.getInstance(props).getStore("imaps");
            store.connect(host, user, appPassword);
            log.info("Conexión IMAP OK: host={}", host);
            return store;
        } catch (MessagingException e) {
            throw new GmailException("No se pudo conectar a Gmail: " + e.getMessage());
        }
    }

    /** Enumera todas las carpetas de la cuenta (ordenadas alfabeticamente). */
    public List<String> listarCarpetas() {
        try (Store store = conectar()) {
            Folder[] carpetas = store.getDefaultFolder().list("**");
            List<String> nombres = new ArrayList<>();
            for (Folder f : carpetas) nombres.add(f.getFullName());
            Collections.sort(nombres);
            log.info("Carpetas listadas: {} carpetas", nombres.size());
            return nombres;
        } catch (MessagingException e) {
            throw new GmailException("Error al listar carpetas: " + e.getMessage());
        }
    }

    /**
     * Lee hasta {@code limite} correos de la carpeta indicada, del más reciente
     * al más antiguo, y los devuelve como CorreoLeido (sin persistir).
     */
    public List<CorreoLeido> listarCorreos(String carpeta, int limite) {
        try (Store store = conectar()) {
            Folder folder = store.getFolder(carpeta);
            if (folder == null || !folder.exists()) {
                throw new GmailException("Carpeta no encontrada: " + carpeta);
            }
            try {
                folder.open(Folder.READ_ONLY);
            } catch (FolderNotFoundException e) {
                return List.of();   // carpeta contenedora tipo [Gmail] sin mensajes propios
            }

            try {
                int total = folder.getMessageCount();
                if (total <= 0) return List.of();
                int desde = Math.max(1, total - limite + 1);
                Message[] mensajes = folder.getMessages(desde, total);

                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.FLAGS);
                fp.add(FetchProfile.Item.ENVELOPE);
                folder.fetch(mensajes, fp);

                List<CorreoLeido> resultado = new ArrayList<>();
                for (int i = mensajes.length - 1; i >= 0; i--) {   // más nuevos primero
                    resultado.add(mapear(mensajes[i], carpeta, folder));
                }
                log.info("Leídos {} correos de '{}'", resultado.size(), carpeta);
                return resultado;
            } finally {
                folder.close(false);
            }
        } catch (GmailException e) {
            throw e;
        } catch (MessagingException e) {
            throw new GmailException("Error al leer correos: " + e.getMessage());
        }
    }

    public int procesarNuevos(String carpeta, int batchSize, Predicate<CorreoLeido> processor) {
        try (Store store = conectar()) {
            Folder folder = store.getFolder(carpeta);
            if (folder == null || !folder.exists()) {
                throw new GmailException("Carpeta no encontrada: " + carpeta);
            }
            folder.open(Folder.READ_WRITE);
            try {
                Message[] mensajes = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                int limit = Math.min(mensajes.length, Math.max(batchSize, 1));
                int procesados = 0;
                for (int i = 0; i < limit; i++) {
                    Message message = mensajes[i];
                    CorreoLeido leido = mapear(message, carpeta, folder);
                    if (processor.test(leido)) {
                        message.setFlag(Flags.Flag.SEEN, true);
                        procesados++;
                    }
                }
                return procesados;
            } finally {
                folder.close(true);
            }
        } catch (GmailException e) {
            throw e;
        } catch (MessagingException e) {
            throw new GmailException("Error en ingesta IMAP: " + e.getMessage());
        }
    }

    /**
     * Convierte un Message de Jakarta Mail en CorreoLeido.
     * La identidad viene del header Message-ID; si no existe se usa el UID
     * de la carpeta y, como último recurso, un UUID aleatorio.
     */
    private CorreoLeido mapear(Message m, String carpeta, Folder folder)
            throws MessagingException {
        String remitente = "desconocido";
        if (m.getFrom() != null && m.getFrom().length > 0) {
            try {
                remitente = MimeUtility.decodeText(m.getFrom()[0].toString());
            } catch (Exception e) {
                remitente = m.getFrom()[0].toString();
            }
        }
        String asunto = "(sin asunto)";
        if (m.getSubject() != null) {
            try {
                asunto = MimeUtility.decodeText(m.getSubject());
            } catch (Exception e) {
                asunto = m.getSubject();
            }
        }
        LocalDateTime fecha = null;
        if (m.getReceivedDate() != null) {
            fecha = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(m.getReceivedDate().getTime()),
                    ZoneId.systemDefault());
        }
        String id = null;
        String[] ids = m.getHeader("Message-ID");
        if (ids != null && ids.length > 0) {
            id = ids[0];
        }
        if (id == null && folder instanceof UIDFolder uf) {
            id = "UID:" + uf.getUID(m);
        }
        if (id == null) id = UUID.randomUUID().toString();

        String cuerpo = "";
        List<AdjuntoLeido> adjuntos = new ArrayList<>();
        try {
            Object contenido = m.getContent();
            cuerpo = extraerTexto(contenido);
            extraerAdjuntos(contenido, adjuntos);
        } catch (Exception e) {
            log.warn("No se pudo leer contenido de messageId={}: {}", id, e.getClass().getSimpleName());
        }
        return new CorreoLeido(id, carpeta, remitente, asunto, fecha,
                snippet(cuerpo), m.isSet(Flags.Flag.SEEN), cuerpo, adjuntos);
    }

    /** Extrae un fragmento plano (sin HTML) de hasta 200 caracteres del cuerpo. */
    private String snippet(String texto) {
        try {
            if (texto == null) return "";
            texto = texto.replaceAll("<[^>]*>", " ")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            return texto.length() > 200 ? texto.substring(0, 200) + "..." : texto;
        } catch (Exception e) {
            return "";
        }
    }

    /** Recorre el cuerpo (String o Multipart) y devuelve el texto plano o HTML. */
    private String extraerTexto(Object contenido) throws Exception {
        if (contenido instanceof String s) return s;
        if (contenido instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) return bp.getContent().toString();
                if (bp.isMimeType("text/html")) return bp.getContent().toString();
                if (bp.getContent() instanceof Multipart) {
                    String anidado = extraerTexto(bp.getContent());
                    if (anidado != null && !anidado.isBlank()) return anidado;
                }
            }
        }
        return "";
    }

    private void extraerAdjuntos(Object contenido, List<AdjuntoLeido> adjuntos) throws Exception {
        if (!(contenido instanceof Multipart mp)) return;
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart parte = mp.getBodyPart(i);
            Object parteContenido = parte.getContent();
            if (parteContenido instanceof Multipart anidado) {
                extraerAdjuntos(anidado, adjuntos);
                continue;
            }
            String disposition = parte.getDisposition();
            String filename = parte.getFileName() != null ? MimeUtility.decodeText(parte.getFileName()) : null;
            boolean esAdjunto = Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition) || filename != null;
            if (esAdjunto && filename != null) {
                byte[] bytes = readAll(parte.getInputStream());
                String mime = parte.getContentType() == null ? "application/octet-stream" : parte.getContentType().split(";")[0].trim().toLowerCase();
                adjuntos.add(new AdjuntoLeido(filename, mime, bytes));
            }
        }
    }

    private byte[] readAll(InputStream in) throws Exception {
        try (in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        }
    }
}