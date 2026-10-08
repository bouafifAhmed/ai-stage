package com.gestionstages.service;

import com.gestionstages.exception.StageBusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureStorageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void stockeEtRelitUneSignaturePng() {
        SignatureStorageService service = new SignatureStorageService(tempDirectory.toString());
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        };
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);

        String storageKey = service.store(7L, "etudiant", base64);
        byte[] loaded = service.load(storageKey);

        assertTrue(storageKey.startsWith("7/etudiant-"));
        assertArrayEquals(png, loaded);
    }

    @Test
    void refuseUneSignatureVide() {
        SignatureStorageService service = new SignatureStorageService(tempDirectory.toString());
        assertThrows(StageBusinessException.class, () -> service.store(1L, "etudiant", " "));
    }
}
