package com.drogia.exception;

/** Excepción para errores del servicio IMAP de Gmail. */
public class GmailException extends RuntimeException {
    public GmailException(String message) {
        super(message);
    }
}