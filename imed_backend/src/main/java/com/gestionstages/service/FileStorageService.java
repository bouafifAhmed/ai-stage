package com.gestionstages.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads/journal")
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, Long stageId) {
        // Validate file type
        String originalFileName = file.getOriginalFilename() != null ? StringUtils.cleanPath(file.getOriginalFilename()) : "";
        String lowerCaseName = originalFileName.toLowerCase();
        if (!lowerCaseName.endsWith(".pdf") && !lowerCaseName.endsWith(".jpg") && !lowerCaseName.endsWith(".png")) {
            throw new IllegalArgumentException("Only PDF, JPG, and PNG files are allowed.");
        }
        
        // Max size is usually handled by Spring Boot properties, but we can double check
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB limit.");
        }

        try {
            if (originalFileName.contains("..")) {
                throw new IllegalArgumentException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            Path stageDir = this.fileStorageLocation.resolve(String.valueOf(stageId));
            Files.createDirectories(stageDir);

            String fileExtension = "";
            int i = originalFileName.lastIndexOf('.');
            if (i > 0) {
                fileExtension = originalFileName.substring(i);
            }
            
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = stageDir.resolve(newFileName);
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/journal/" + stageId + "/" + newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public org.springframework.core.io.Resource loadFileAsResource(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Chemin de fichier invalide");
        }
        String clean = relativePath.replace("/uploads/journal/", "").replace("\\", "/");
        Path filePath = this.fileStorageLocation.resolve(clean).normalize();
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            return new org.springframework.core.io.FileSystemResource(filePath.toFile());
        } else {
            throw new IllegalArgumentException("Fichier introuvable");
        }
    }
}
