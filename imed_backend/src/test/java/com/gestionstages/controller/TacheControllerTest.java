package com.gestionstages.controller;

import com.gestionstages.dto.CreateTacheDTO;
import com.gestionstages.dto.ProgressionDTO;
import com.gestionstages.dto.TacheResponseDTO;
import com.gestionstages.dto.UpdateTacheDTO;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.Role;
import com.gestionstages.model.Tache;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.StageRepository;
import com.gestionstages.repository.TacheRepository;
import com.gestionstages.service.FileStorageService;
import com.gestionstages.service.JournalPdfService;
import com.gestionstages.service.TacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Journal de stage : codes HTTP et contrôle d'accès des endpoints.
 */
class TacheControllerTest {
    private TacheService tacheService;
    private FileStorageService fichiers;
    private TacheRepository taches;
    private StageRepository stages;
    private JournalPdfService pdf;
    private TacheController controller;
    private Utilisateur etudiant;

    @BeforeEach
    void setUp() {
        tacheService = mock(TacheService.class);
        fichiers = mock(FileStorageService.class);
        taches = mock(TacheRepository.class);
        stages = mock(StageRepository.class);
        pdf = mock(JournalPdfService.class);
        controller = new TacheController(tacheService, fichiers, taches, stages, pdf);
        etudiant = new Utilisateur();
        etudiant.setId(2L);
        etudiant.setRole(Role.ETUDIANT);
    }

    @Test
    void laCreationEtLaModificationTraduisentLesErreursMetier() {
        CreateTacheDTO creation = new CreateTacheDTO();
        UpdateTacheDTO modification = new UpdateTacheDTO();
        when(tacheService.creerTache(30L, creation, 2L)).thenReturn(new TacheResponseDTO());
        when(tacheService.creerTache(31L, creation, 2L)).thenThrow(new SecurityException("Pas votre stage"));
        when(tacheService.modifierTache(50L, modification, 2L)).thenThrow(new IllegalArgumentException("Non rejetée"));

        assertEquals(201, controller.creerTache(30L, creation, etudiant).getStatusCode().value());
        assertEquals(403, controller.creerTache(31L, creation, etudiant).getStatusCode().value());
        assertEquals(400, controller.modifierTache(30L, 50L, modification, etudiant).getStatusCode().value());
        assertEquals(401, controller.creerTache(30L, creation, null).getStatusCode().value());
    }

    @Test
    void laListeEtLaProgressionVerifientLAccesAuStage() {
        when(tacheService.listerTachesParStage(30L, 2L, "ETUDIANT")).thenReturn(List.of());
        when(tacheService.calculerProgression(30L)).thenReturn(new ProgressionDTO());
        when(tacheService.listerTachesParStage(31L, 2L, "ETUDIANT")).thenThrow(new IllegalArgumentException("Stage introuvable"));

        assertEquals(200, controller.listerTaches(30L, etudiant).getStatusCode().value());
        assertEquals(200, controller.calculerProgression(30L, etudiant).getStatusCode().value());
        assertEquals(404, controller.listerTaches(31L, etudiant).getStatusCode().value());
        assertEquals(404, controller.calculerProgression(31L, etudiant).getStatusCode().value());
        verify(tacheService, never()).calculerProgression(31L);
    }

    @Test
    void lUploadVerifieLAccesAvantDeStockerLeFichier() {
        MockMultipartFile fichier = new MockMultipartFile("file", "preuve.pdf", "application/pdf", new byte[]{1});
        when(fichiers.storeFile(fichier, 30L)).thenReturn("/uploads/journal/30/x.pdf");
        when(tacheService.listerTachesParStage(31L, 2L, "ETUDIANT")).thenThrow(new SecurityException("Pas votre stage"));

        ResponseEntity<?> ok = controller.uploadPieceJointe(30L, fichier, etudiant);
        assertEquals(Map.of("chemin", "/uploads/journal/30/x.pdf"), ok.getBody());
        assertEquals(403, controller.uploadPieceJointe(31L, fichier, etudiant).getStatusCode().value());
        verify(fichiers, never()).storeFile(any(), eq(31L));
    }

    @Test
    void laPieceJointeEstServieAvecSonTypeDeContenu() {
        Tache tache = tache(30L, "/uploads/journal/30/x.PDF");
        Resource resource = new ByteArrayResource(new byte[]{1});
        when(taches.findById(50L)).thenReturn(Optional.of(tache));
        when(fichiers.loadFileAsResource("/uploads/journal/30/x.PDF")).thenReturn(resource);

        ResponseEntity<Resource> reponse = controller.telechargerPieceJointe(30L, 50L, etudiant);

        assertEquals(200, reponse.getStatusCode().value());
        assertEquals("application/pdf", reponse.getHeaders().getContentType().toString());
        assertSame(resource, reponse.getBody());
    }

    @Test
    void uneTacheSansPieceJointeRenvoie404() {
        when(taches.findById(50L)).thenReturn(Optional.of(tache(30L, " ")));

        assertEquals(404, controller.telechargerPieceJointe(30L, 50L, etudiant).getStatusCode().value());
    }

    @Test
    void onNePeutPasLireLaPieceJointeDUneTacheDUnAutreStage() {
        // L'étudiant a accès au stage 30, mais la tâche 51 appartient au stage 99.
        when(tacheService.listerTachesParStage(30L, 2L, "ETUDIANT")).thenReturn(List.of());
        when(taches.findById(51L)).thenReturn(Optional.of(tache(99L, "/uploads/journal/99/secret.pdf")));

        ResponseEntity<Resource> reponse = controller.telechargerPieceJointe(30L, 51L, etudiant);

        assertEquals(404, reponse.getStatusCode().value());
        verify(fichiers, never()).loadFileAsResource(anyString());
    }

    @Test
    void leJournalPdfEstRefuseAQuiNAPasAccesAuStage() {
        when(tacheService.listerTachesParStage(99L, 2L, "ETUDIANT")).thenThrow(new SecurityException("Pas votre stage"));
        when(stages.findByIdWithDetails(99L)).thenReturn(Optional.of(new Candidature()));
        when(tacheService.calculerProgression(99L)).thenReturn(new ProgressionDTO());

        ResponseEntity<?> reponse = controller.telechargerJournalPdf(99L, etudiant);

        assertEquals(403, reponse.getStatusCode().value());
        verify(pdf, never()).genererPdfJournal(any(), any(), anyDouble());
    }

    @Test
    void leJournalPdfEstGenerePourUnUtilisateurAutorise() {
        Candidature stage = new Candidature();
        when(stages.findByIdWithDetails(30L)).thenReturn(Optional.of(stage));
        when(taches.findByStageIdOrderByDateAsc(30L)).thenReturn(List.of());
        ProgressionDTO progression = new ProgressionDTO();
        when(tacheService.calculerProgression(30L)).thenReturn(progression);
        when(pdf.genererPdfJournal(eq(stage), anyList(), anyDouble())).thenReturn(new byte[]{37, 80, 68, 70});

        ResponseEntity<?> reponse = controller.telechargerJournalPdf(30L, etudiant);

        assertEquals(200, reponse.getStatusCode().value());
        assertEquals("application/pdf", reponse.getHeaders().getContentType().toString());
        verify(tacheService).listerTachesParStage(30L, 2L, "ETUDIANT");
    }

    private Tache tache(Long stageId, String pieceJointe) {
        Candidature stage = new Candidature();
        stage.setId(stageId);
        Tache tache = new Tache();
        tache.setStage(stage);
        tache.setPieceJointe(pieceJointe);
        return tache;
    }
}
