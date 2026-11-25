package com.drogia.modules.cotizaciones.service;

import com.drogia.exception.ApiException;
import com.drogia.modules.cotizaciones.dto.request.EnvioCotizacionRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Envío de cotizaciones por correo con el PDF generado en el frontend. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CotizacionMailService {

    private final JavaMailSender mailSender;

    @Value("${gmail.user}")
    private String remitente;

    /** Envía el correo con el PDF adjunto. */
    public void enviar(EnvioCotizacionRequest request) {
        byte[] pdf = decodificarPdf(request.pdfBase64());
        String nombre = request.pdfFileName() == null || request.pdfFileName().isBlank()
                ? "cotizacion.pdf"
                : request.pdfFileName().replaceAll("[^a-zA-Z0-9._-]", "_");

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(request.to().trim());
            helper.setSubject(request.subject() == null ? "Cotización" : request.subject());
            helper.setText(request.body() == null ? "" : request.body());
            if (pdf.length > 0) {
                helper.addAttachment(nombre, new ByteArrayResource(pdf), "application/pdf");
            }
            mailSender.send(mensaje);
            log.info("Cotización enviada a {}", request.to());
        } catch (MessagingException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "No se pudo enviar el correo: " + ex.getMessage());
        }
    }

    private byte[] decodificarPdf(String pdfBase64) {
        if (pdfBase64 == null || pdfBase64.isBlank()) {
            return new byte[0];
        }
        String valor = pdfBase64.contains(",")
                ? pdfBase64.substring(pdfBase64.indexOf(',') + 1)
                : pdfBase64;
        try {
            return Base64.getDecoder().decode(valor);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El PDF recibido no es válido");
        }
    }
}
