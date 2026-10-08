package com.gestionstages.service;

import com.gestionstages.exception.StageBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class CvStorageService {
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;

    private final Path storageDirectory;

    public CvStorageService(@Value("${app.upload.cv-dir:uploads/cv}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public void store(Long userId, MultipartFile file) {
        validate(file);
        try {
            Files.createDirectories(storageDirectory);
            Files.copy(file.getInputStream(), pathFor(userId), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Le CV n'a pas pu être enregistré"
            );
        }
    }

    public Resource load(Long userId) {
        Path path = pathFor(userId);
        if (!Files.isRegularFile(path)) {
            throw new StageBusinessException(HttpStatus.NOT_FOUND, "Aucun CV n'a été déposé");
        }
        return new FileSystemResource(path.toFile());
    }

    public void delete(Long userId) {
        try {
            Files.deleteIfExists(pathFor(userId));
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Le CV n'a pas pu être supprimé"
            );
        }
    }

    public String safeOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return "cv.pdf";
        }
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("\\p{Cntrl}", "_")
                .trim();
        if (filename.isBlank()) {
            return "cv.pdf";
        }
        if (!filename.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            filename += ".pdf";
        }
        return filename.length() > 255 ? filename.substring(filename.length() - 255) : filename;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StageBusinessException(HttpStatus.BAD_REQUEST, "Veuillez sélectionner un fichier PDF");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new StageBusinessException(HttpStatus.CONTENT_TOO_LARGE, "Le CV ne doit pas dépasser 20 Mo");
        }
        if (!hasPdfSignature(file)) {
            throw new StageBusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Le CV doit être un fichier PDF valide");
        }
    }

    private boolean hasPdfSignature(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(PDF_SIGNATURE.length);
            return java.util.Arrays.equals(signature, PDF_SIGNATURE);
        } catch (IOException exception) {
            return false;
        }
    }

    private Path pathFor(Long userId) {
        return storageDirectory.resolve(userId + ".pdf");
    }
}
