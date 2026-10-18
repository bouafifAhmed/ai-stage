package com.gestionstages.service;

import com.gestionstages.model.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StageRapportPdfServiceTest {
    private SignatureStorageService signatures;
    private StageRapportPdfService service;
    private Candidature stage;

    @BeforeEach
    void setUp() {
        signatures = mock(SignatureStorageService.class);
        service = new StageRapportPdfService(signatures);

        Entreprise acme = new Entreprise();
        acme.setNom("Acme");
        OffreStage offre = new OffreStage();
        offre.setTitre("Stage Java");
        offre.setEntreprise(acme);
        Utilisateur etudiant = new Utilisateur();
        etudiant.setPrenom("Lina");
        etudiant.setNom("Doe");
        stage = new Candidature();
        stage.setOffre(offre);
        stage.setEtudiant(etudiant);
        stage.setStatutStage(StatutStage.ACTIF);
        stage.setDateCandidature(LocalDateTime.of(2026, 9, 1, 10, 0));
    }

    @Test
    void leRapportResumeLeStageEtLesTaches() throws IOException {
        String texte = texte(service.generer(stage, List.of(
                tache("Rapport final", StatutTacheAssignee.VALIDEE),
                tache("Maquette", StatutTacheAssignee.ASSIGNEE))));

        assertTrue(texte.contains("Rapport de stage"));
        assertTrue(texte.contains("Stage : Stage Java"));
        assertTrue(texte.contains("Entreprise : Acme"));
        assertTrue(texte.contains("Lina Doe"));
        assertTrue(texte.contains("Total : 2"));
        assertTrue(texte.contains("Validees : 1"));
        assertTrue(texte.contains("Assignees : 1"));
        assertTrue(texte.contains("Rapport final"));
        verifyNoInteractions(signatures);
    }

    @Test
    void leRapportDUnStageClotureIntegreLesDeuxSignatures() throws IOException {
        Utilisateur encadrant = new Utilisateur();
        encadrant.setPrenom("Sami");
        encadrant.setNom("RH");
        stage.setEncadrant(encadrant);
        stage.setStatutStage(StatutStage.CLOTURE);
        stage.setSignatureEtudiantPath("signatures/etudiant.png");
        stage.setSignatureEncadrantPath("signatures/encadrant.png");
        stage.setDateSignatureEtudiant(LocalDateTime.of(2026, 10, 1, 9, 0));
        stage.setDateSignatureEncadrant(LocalDateTime.of(2026, 10, 2, 9, 0));
        stage.setDateCloture(LocalDateTime.of(2026, 10, 2, 9, 0));
        when(signatures.load(anyString())).thenReturn(png());

        byte[] pdf = service.generer(stage, List.of());

        String texte = texte(pdf);
        assertTrue(texte.contains("Encadrant : Sami RH"));
        assertTrue(texte.contains("Statut stage : CLOTURE"));
        assertTrue(texte.contains("Cloture le"));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            int images = 0;
            for (var ignored : document.getPage(0).getResources().getXObjectNames()) {
                images++;
            }
            assertEquals(2, images);
        }
        verify(signatures).load("signatures/etudiant.png");
        verify(signatures).load("signatures/encadrant.png");
    }

    @Test
    void beaucoupDeTachesNeFontPasDeborderLaPage() throws IOException {
        List<TacheAssignee> taches = new ArrayList<>();
        IntStream.range(0, 80).forEach(i -> taches.add(tache("Tâche " + i, StatutTacheAssignee.TERMINEE)));

        byte[] pdf = service.generer(stage, taches);

        assertTrue(new String(pdf, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF"));
        String texte = texte(pdf);
        assertTrue(texte.contains("Total : 80"));
        assertTrue(texte.contains("Terminees : 80"));
    }

    private TacheAssignee tache(String titre, StatutTacheAssignee statut) {
        TacheAssignee tache = new TacheAssignee();
        tache.setTitre(titre);
        tache.setDateEcheance(LocalDate.of(2026, 10, 15));
        tache.setStatut(statut);
        return tache;
    }

    // Le service retire les accents avant d'écrire (police Helvetica standard) : « Validées » devient « Validees ».
    private String texte(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private byte[] png() throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", sortie);
        return sortie.toByteArray();
    }
}
