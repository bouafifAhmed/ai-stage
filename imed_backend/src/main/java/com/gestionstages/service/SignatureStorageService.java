package com.gestionstages.service;

import com.gestionstages.exception.StageBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SignatureStorageService {
    private static final Pattern DATA_URL = Pattern.compile(
            "^data:image/png;base64,(.+)$",
            Pattern.DOTALL
    );
    private static final int MAX_BYTES = 512 * 1024;

    private final Path storageDirectory;

    public SignatureStorageService(
            @Value("${app.upload.signature-dir:uploads/signatures}") String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String store(Long stageId, String role, String signatureBase64) {
        byte[] png = decode(signatureBase64);
        if (png.length > MAX_BYTES) {
            throw new StageBusinessException(
                    HttpStatus.CONTENT_TOO_LARGE,
                    "La signature ne doit pas dépasser 512 Ko"
            );
        }
        String storageKey = stageId + "/" + role + "-" + UUID.randomUUID() + ".png";
        Path destination = resolve(storageKey);
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, png);
            return storageKey;
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La signature n'a pas pu être enregistrée"
            );
        }
    }

    public byte[] load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new StageBusinessException(HttpStatus.NOT_FOUND, "Signature introuvable");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La signature n'a pas pu être lue"
            );
        }
    }

    public void delete(String storageKey) {
        if (storageKey == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "L'ancienne signature n'a pas pu être supprimée"
            );
        }
    }

    private byte[] decode(String signatureBase64) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            throw new StageBusinessException(HttpStatus.BAD_REQUEST, "La signature est obligatoire");
        }
        String payload = signatureBase64.trim();
        var matcher = DATA_URL.matcher(payload);
        if (matcher.matches()) {
            payload = matcher.group(1);
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(payload.replaceAll("\\s", ""));
            if (decoded.length < 8) {
                throw new StageBusinessException(HttpStatus.BAD_REQUEST, "Signature invalide");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new StageBusinessException(HttpStatus.BAD_REQUEST, "Signature invalide");
        }
    }

    private Path resolve(String storageKey) {
        Path path = storageDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new StageBusinessException(HttpStatus.BAD_REQUEST, "Référence de signature invalide");
        }
        return path;
    }
}
