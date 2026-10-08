package com.gestionstages.service;

import com.gestionstages.model.Candidature;
import com.gestionstages.model.Tache;
import com.gestionstages.model.StatutApprobation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JournalPdfServiceTest {

    @Test
    void testGenererPdfJournalAvecTachesSimples() {
        // Mock SignatureStorageService
        SignatureStorageService mockStorage = Mockito.mock(SignatureStorageService.class);
        JournalPdfService service = new JournalPdfService(mockStorage);

        // Stage minimal sans offre ni encadrant
        Candidature stage = new Candidature();

        // Tâche avec des caractères accentués
        Tache tache = new Tache();
        tache.setDate(LocalDate.now());
        tache.setTitre("Tâche de développement");
        tache.setDescription("Description avec accents : é, è, à, ê, ù");
        tache.setStatutApprobation(StatutApprobation.EN_ATTENTE);

        byte[] pdf = service.genererPdfJournal(stage, List.of(tache), 50.0);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0, "Le PDF doit contenir des données");
        System.out.println("PDF généré avec succès : " + pdf.length + " octets");
    }

    @Test
    void testGenererPdfJournalSansTaches() {
        SignatureStorageService mockStorage = Mockito.mock(SignatureStorageService.class);
        JournalPdfService service = new JournalPdfService(mockStorage);

        Candidature stage = new Candidature();

        byte[] pdf = service.genererPdfJournal(stage, List.of(), 0.0);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        System.out.println("PDF vide généré avec succès : " + pdf.length + " octets");
    }
}
