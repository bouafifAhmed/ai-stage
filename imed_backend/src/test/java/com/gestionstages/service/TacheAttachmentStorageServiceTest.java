package com.gestionstages.service;

import com.gestionstages.exception.StageBusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacheAttachmentStorageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void stockeSousUnNomServeurEtConserveUnNomClientSur() throws Exception {
        TacheAttachmentStorageService service =
                new TacheAttachmentStorageService(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../rapport final.pdf",
                "application/pdf",
                "contenu".getBytes()
        );

        TacheAttachmentStorageService.StoredAttachment stored = service.store(42L, file);
        TacheAttachmentStorageService.AttachmentDocument loaded = service.load(stored.storageKey());

        assertTrue(stored.storageKey().startsWith("42/"));
        assertFalse(stored.storageKey().contains(".."));
        assertEquals("rapport final.pdf", stored.originalFilename());
        assertEquals("rapport final.pdf", loaded.filename());
        assertArrayEquals(file.getBytes(), loaded.resource().getContentAsByteArray());
    }

    @Test
    void refuseUneExtensionExecutable() {
        TacheAttachmentStorageService service =
                new TacheAttachmentStorageService(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "attaque.exe",
                "application/octet-stream",
                "MZ".getBytes()
        );

        StageBusinessException exception = assertThrows(
                StageBusinessException.class,
                () -> service.store(42L, file)
        );

        assertEquals(415, exception.getStatus().value());
    }
}
