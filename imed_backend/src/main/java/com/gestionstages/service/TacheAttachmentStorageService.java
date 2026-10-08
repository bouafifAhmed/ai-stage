package com.gestionstages.service;

import com.gestionstages.exception.StageBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TacheAttachmentStorageService {
    private static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "bat", "cmd", "com", "cpl", "exe", "hta", "html", "htm", "jar",
            "js", "jse", "lnk", "msi", "ps1", "scr", "svg", "vbs", "wsf"
    );

    private final Path storageDirectory;

    public TacheAttachmentStorageService(
            @Value("${app.upload.tache-dir:uploads/taches-assignees}") String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public StoredAttachment store(Long tacheId, MultipartFile file) {
        validate(file);
        String originalFilename = safeOriginalFilename(file);
        String storageKey = tacheId + "/" + UUID.randomUUID() + "--" + originalFilename;
        Path destination = resolve(storageKey);
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredAttachment(storageKey, originalFilename);
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La pièce jointe n'a pas pu être enregistrée"
            );
        }
    }

    public AttachmentDocument load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new StageBusinessException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable");
        }
        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (IOException exception) {
            contentType = null;
        }
        return new AttachmentDocument(
                new FileSystemResource(path.toFile()),
                originalFilename(storageKey),
                contentType == null ? "application/octet-stream" : contentType
        );
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
                    "L'ancienne pièce jointe n'a pas pu être supprimée"
            );
        }
    }

    public String originalFilename(String storageKey) {
        if (storageKey == null) {
            return null;
        }
        String storedName = Path.of(storageKey.replace('\\', '/')).getFileName().toString();
        int separator = storedName.indexOf("--");
        return separator >= 0 && separator + 2 < storedName.length()
                ? storedName.substring(separator + 2)
                : "piece-jointe";
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StageBusinessException(HttpStatus.BAD_REQUEST, "La pièce jointe est vide");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new StageBusinessException(
                    HttpStatus.CONTENT_TOO_LARGE,
                    "La pièce jointe ne doit pas dépasser 20 Mo"
            );
        }
        String filename = safeOriginalFilename(file);
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new StageBusinessException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Ce type de pièce jointe n'est pas autorisé"
            );
        }
    }

    private String safeOriginalFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "piece-jointe";
        }
        String normalized = original.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_")
                .replaceAll("\\s+", " ")
                .trim();
        filename = filename.replaceFirst("^[. ]+", "").replaceFirst("[. ]+$", "");
        if (filename.isBlank()) {
            return "piece-jointe";
        }
        return filename.length() > 180 ? filename.substring(filename.length() - 180) : filename;
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new StageBusinessException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable");
        }
        Path path = storageDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new StageBusinessException(HttpStatus.BAD_REQUEST, "Référence de pièce jointe invalide");
        }
        return path;
    }

    public record StoredAttachment(String storageKey, String originalFilename) {
    }

    public record AttachmentDocument(Resource resource, String filename, String contentType) {
    }
}
