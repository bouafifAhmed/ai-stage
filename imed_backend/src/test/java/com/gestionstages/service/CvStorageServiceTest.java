package com.gestionstages.service;

import com.gestionstages.exception.StageBusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CvStorageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void enregistreEtChargeUnPdfValide() throws Exception {
        CvStorageService service = new CvStorageService(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mon-cv.pdf",
                "application/pdf",
                "%PDF-1.7 contenu".getBytes()
        );

        service.store(42L, file);

        assertTrue(service.load(42L).exists());
        assertArrayEquals(file.getBytes(), service.load(42L).getContentAsByteArray());
        assertEquals("mon-cv.pdf", service.safeOriginalFilename(file));
    }

    @Test
    void refuseUnFichierQuiNestPasUnVraiPdf() {
        CvStorageService service = new CvStorageService(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "faux.pdf",
                "application/pdf",
                "contenu texte".getBytes()
        );

        StageBusinessException exception = assertThrows(
                StageBusinessException.class,
                () -> service.store(42L, file)
        );

        assertEquals(415, exception.getStatus().value());
        assertFalse(Files.exists(tempDirectory.resolve("42.pdf")));
    }
}
