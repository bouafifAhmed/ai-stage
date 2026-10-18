package com.gestionstages.service;

import com.gestionstages.dto.TacheAssigneeResponseDTO;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.*;
import com.gestionstages.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Lecture des tâches assignées et de leurs pièces jointes selon le rôle.
 */
class TacheAssigneeLectureTest {
    private TacheAssigneeRepository taches;
    private CandidatureRepository candidatures;
    private TacheAttachmentStorageService pieces;
    private TacheAssigneeService service;
    private Candidature stage;
    private Utilisateur etudiant;
    private Utilisateur encadrant;
    private TacheAssignee tache;

    @BeforeEach
    void setUp() {
        taches = mock(TacheAssigneeRepository.class);
        candidatures = mock(CandidatureRepository.class);
        pieces = mock(TacheAttachmentStorageService.class);
        service = new TacheAssigneeService(taches, candidatures, mock(EntrepriseRepository.class),
                mock(UserRepository.class), pieces, mock(NotificationEtudiantRepository.class));

        Entreprise acme = new Entreprise();
        acme.setId(10L);
        etudiant = utilisateur(2L, Role.ETUDIANT);
        encadrant = utilisateur(3L, Role.ENTREPRISE);
        encadrant.setEntreprise(acme);
        OffreStage offre = new OffreStage();
        offre.setEntreprise(acme);
        stage = new Candidature();
        stage.setId(30L);
        stage.setEtudiant(etudiant);
        stage.setOffre(offre);
        stage.setStatut(Candidature.StatutCandidature.ACCEPTEE);

        tache = new TacheAssignee();
        tache.setId(50L);
        tache.setStage(stage);
        tache.setEncadrant(encadrant);
        tache.setTitre("Rapport");
        tache.setDateEcheance(LocalDate.now().plusDays(3));
        tache.setStatut(StatutTacheAssignee.TERMINEE);
        tache.setPieceJointeEtudiant("taches/50/abc.pdf");

        when(candidatures.findById(30L)).thenReturn(Optional.of(stage));
        when(taches.findById(50L)).thenReturn(Optional.of(tache));
        when(taches.findByStageId(30L)).thenReturn(List.of(tache));
        when(taches.findByStageIdAndStatut(30L, StatutTacheAssignee.VALIDEE)).thenReturn(List.of());
        when(pieces.originalFilename("taches/50/abc.pdf")).thenReturn("rapport.pdf");
    }

    @Test
    void lEtudiantEtLEncadrantVoientLesTachesDuStage() {
        List<TacheAssigneeResponseDTO> vueEtudiant = service.listerTachesParStage(30L, null, etudiant);
        List<TacheAssigneeResponseDTO> vueEncadrant = service.listerTachesParStage(30L, null, encadrant);

        assertEquals(1, vueEtudiant.size());
        assertEquals("rapport.pdf", vueEtudiant.get(0).pieceJointeEtudiant());
        assertEquals(1, vueEncadrant.size());
        assertTrue(service.listerTachesParStage(30L, StatutTacheAssignee.VALIDEE, etudiant).isEmpty());
    }

    @Test
    void leChefDeDepartementPeutLireMaisPasLeSuperAdmin() {
        assertEquals(1, service.listerTachesParStage(30L, null, utilisateur(5L, Role.CHEF_DEPT_STAGE)).size());

        StageBusinessException erreur = assertThrows(StageBusinessException.class,
                () -> service.listerTachesParStage(30L, null, utilisateur(6L, Role.SUPER_ADMIN)));
        assertEquals(HttpStatus.FORBIDDEN, erreur.getStatus());
    }

    @Test
    void unAutreEtudiantOuUneAutreEntrepriseNeVoientRien() {
        assertThrows(StageBusinessException.class,
                () -> service.listerTachesParStage(30L, null, utilisateur(9L, Role.ETUDIANT)));

        Entreprise autre = new Entreprise();
        autre.setId(99L);
        Utilisateur intrus = utilisateur(8L, Role.ENTREPRISE);
        intrus.setEntreprise(autre);
        assertThrows(StageBusinessException.class, () -> service.listerTachesParStage(30L, null, intrus));
    }

    @Test
    void laPieceJointeEstTelechargeableParLesPersonnesAutorisees() {
        TacheAttachmentStorageService.AttachmentDocument document = new TacheAttachmentStorageService.AttachmentDocument(
                new ByteArrayResource(new byte[]{1}), "rapport.pdf", "application/pdf");
        when(pieces.load("taches/50/abc.pdf")).thenReturn(document);

        assertSame(document, service.telechargerPieceJointe(30L, 50L, encadrant));
    }

    @Test
    void uneTacheSansPieceJointeOuDUnAutreStageRenvoie404() {
        tache.setPieceJointeEtudiant(null);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(StageBusinessException.class,
                () -> service.telechargerPieceJointe(30L, 50L, etudiant)).getStatus());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(StageBusinessException.class,
                () -> service.telechargerPieceJointe(31L, 50L, etudiant)).getStatus());
        when(taches.findById(51L)).thenReturn(Optional.empty());
        assertThrows(StageBusinessException.class, () -> service.telechargerPieceJointe(30L, 51L, etudiant));
    }

    @Test
    void unStageInexistantOuNonAccepteEstRefuse() {
        when(candidatures.findById(77L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(StageBusinessException.class,
                () -> service.listerTachesParStage(77L, null, etudiant)).getStatus());

        stage.setStatut(Candidature.StatutCandidature.EN_ATTENTE);
        assertThrows(StageBusinessException.class, () -> service.listerTachesParStage(30L, null, etudiant));
    }

    private Utilisateur utilisateur(Long id, Role role) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setRole(role);
        u.setPrenom("Prenom" + id);
        u.setNom("Nom" + id);
        return u;
    }
}
