package com.drogia.modules.correos.service.storage;

public record StoredFile(String storagePath, String sha256, long size) {
}
