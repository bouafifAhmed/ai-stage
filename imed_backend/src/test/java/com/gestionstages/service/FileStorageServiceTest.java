package com.gestionstages.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {
    private static final long STAGE_ID = 990_001L;

    private FileStorageService service;
    private Path dossierStage;
    private Path fichierHorsJournal;

    @BeforeEach
    void setUp() {
        service = new FileStorageService();
        dossierStage = Paths.get("uploads/journal").toAbsolutePath().normalize().resolve(String.valueOf(STAGE_ID));
        fichierHorsJournal = Paths.get("uploads").toAbsolutePath().normalize()
                .resolve("hors-journal-" + UUID.randomUUID() + ".txt");
    }

    @AfterEach
    void nettoyer() throws IOException {
        Files.deleteIfExists(fichierHorsJournal);
        if (Files.exists(dossierStage)) {
            try (Stream<Path> fichiers = Files.walk(dossierStage)) {
                fichiers.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    void stockeUnPdfSousUnNomGenereEtLeRelit() throws IOException {
        MockMultipartFile fichier = new MockMultipartFile("file", "rapport.PDF", "application/pdf",
                "%PDF-1.7 contenu".getBytes(StandardCharsets.UTF_8));

        String chemin = service.storeFile(fichier, STAGE_ID);

        assertTrue(chemin.startsWith("/uploads/journal/" + STAGE_ID + "/"));
        assertTrue(chemin.endsWith(".PDF"));
        assertFalse(chemin.contains("rapport"));
        Resource relu = service.loadFileAsResource(chemin);
        assertEquals("%PDF-1.7 contenu", new String(relu.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void refuseUneExtensionNonAutorisee() {
        MockMultipartFile fichier = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> service.storeFile(fichier, STAGE_ID));
    }

    @Test
    void refuseUnFichierDePlusDeCinqMo() {
        MockMultipartFile fichier = new MockMultipartFile("file", "photo.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> service.storeFile(fichier, STAGE_ID));
        assertTrue(erreur.getMessage().contains("5MB"));
    }

    @Test
    void refuseUnNomDeFichierAvecRemonteeDeDossier() {
        MockMultipartFile fichier = new MockMultipartFile("file", "../../evil.pdf", "application/pdf", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> service.storeFile(fichier, STAGE_ID));
    }

    @Test
    void refuseUnCheminVideOuUnFichierInexistant() {
        assertThrows(IllegalArgumentException.class, () -> service.loadFileAsResource(" "));
        assertThrows(IllegalArgumentException.class,
                () -> service.loadFileAsResource("/uploads/journal/" + STAGE_ID + "/absent.pdf"));
    }

    @Test
    void neLitJamaisUnFichierEnDehorsDuDossierDuJournal() throws IOException {
        // La pièce jointe d'une tâche du journal est une chaîne envoyée par le client :
        // elle ne doit pas permettre de lire un autre fichier du serveur.
        Files.writeString(fichierHorsJournal, "secret");
        String tentative = "/uploads/journal/../" + fichierHorsJournal.getFileName();

        assertThrows(IllegalArgumentException.class, () -> service.loadFileAsResource(tentative));
    }
}
