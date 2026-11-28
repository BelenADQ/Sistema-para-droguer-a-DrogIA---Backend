package com.drogia.modules.correos.service.storage;

import com.drogia.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path basePath;

    public DocumentStorageService(@Value("${storage.base-path:./storage}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file, String folder) {
        try {
            Files.createDirectories(basePath.resolve(folder));
            String safeName = file.getOriginalFilename() == null ? "archivo" : file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");
            String filename = UUID.randomUUID() + "_" + safeName;
            Path relative = Path.of(folder).resolve(filename);
            Path target = basePath.resolve(relative).normalize();
            if (!target.startsWith(basePath)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ruta de archivo inválida");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = file.getInputStream(); DigestInputStream din = new DigestInputStream(in, digest)) {
                Files.copy(din, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha = HexFormat.of().formatHex(digest.digest());
            return new StoredFile(relative.toString().replace('\\', '/'), sha, file.getSize());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar archivo: " + e.getMessage());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error guardando archivo: " + e.getMessage());
        }
    }

    public Path resolve(String storagePath) {
        Path target = basePath.resolve(storagePath).normalize();
        if (!target.startsWith(basePath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ruta inválida");
        }
        return target;
    }

    public StoredFile storeBytes(String filename, String mimeType, byte[] bytes, String folder) {
        return store(new ByteArrayMultipartFile(filename, mimeType, bytes), folder);
    }
}
