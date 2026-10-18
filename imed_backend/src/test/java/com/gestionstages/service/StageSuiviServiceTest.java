package com.gestionstages.service;

import com.gestionstages.dto.StageSuiviDTOs.EcheanceCalendrierDTO;
import com.gestionstages.dto.StageSuiviDTOs.SignerStageRequest;
import com.gestionstages.dto.StageSuiviDTOs.StageClotureResponseDTO;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.*;
import com.gestionstages.repository.CandidatureRepository;
import com.gestionstages.repository.TacheAssigneeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StageSuiviServiceTest {
    private CandidatureRepository candidatures;
    private TacheAssigneeRepository taches;
    private SignatureStorageService signatures;
    private StageRapportPdfService rapports;
    private StageSuiviService service;

    private Utilisateur etudiant;
    private Utilisateur encadrant;
    private Candidature stage;

    @BeforeEach
    void setUp() {
        candidatures = mock(CandidatureRepository.class);
        taches = mock(TacheAssigneeRepository.class);
        signatures = mock(SignatureStorageService.class);
        rapports = mock(StageRapportPdfService.class);
        service = new StageSuiviService(candidatures, taches, signatures, rapports);

        Entreprise acme = new Entreprise();
        acme.setId(10L);
        acme.setNom("Acme");
        etudiant = utilisateur(2L, Role.ETUDIANT);
        encadrant = utilisateur(3L, Role.ENTREPRISE);
        encadrant.setEntreprise(acme);

        OffreStage offre = new OffreStage();
        offre.setId(20L);
        offre.setTitre("Stage Java");
        offre.setEntreprise(acme);
        stage = new Candidature();
        stage.setId(30L);
        stage.setEtudiant(etudiant);
        stage.setOffre(offre);
        stage.setStatut(Candidature.StatutCandidature.ACCEPTEE);
        stage.setStatutStage(StatutStage.ACTIF);

        when(candidatures.findById(30L)).thenReturn(Optional.of(stage));
        when(candidatures.findByIdForUpdate(30L)).thenReturn(Optional.of(stage));
        when(signatures.store(eq(30L), anyString(), anyString()))
                .thenAnswer(inv -> "signatures/30/" + inv.getArgument(1) + ".png");
    }

    @Test
    void lEtudiantSigneSansCloturerTantQueLEncadrantNAPasSigne() {
        StageClotureResponseDTO etat = service.signerEtudiant(30L, new SignerStageRequest("data"), etudiant);

        assertTrue(etat.signatureEtudiantPresente());
        assertFalse(etat.signatureEncadrantPresente());
        assertEquals(StatutStage.ACTIF, etat.statutStage());
        assertNull(etat.dateCloture());
        assertFalse(etat.peutSignerEtudiant());
        verify(candidatures).save(stage);
    }

    @Test
    void laDeuxiemeSignatureClotureLeStage() {
        service.signerEtudiant(30L, new SignerStageRequest("data"), etudiant);

        StageClotureResponseDTO etat = service.signerEncadrant(30L, new SignerStageRequest("data"), encadrant);

        assertEquals(StatutStage.CLOTURE, etat.statutStage());
        assertNotNull(etat.dateCloture());
        assertSame(encadrant, stage.getEncadrant());
    }

    @Test
    void refuseUneDeuxiemeSignatureEtudiant() {
        service.signerEtudiant(30L, new SignerStageRequest("data"), etudiant);

        StageBusinessException erreur = assertThrows(StageBusinessException.class,
                () -> service.signerEtudiant(30L, new SignerStageRequest("data"), etudiant));
        assertEquals(HttpStatus.BAD_REQUEST, erreur.getStatus());
    }

    @Test
    void refuseLaSignatureDUnAutreEtudiantOuDUnMauvaisRole() {
        Utilisateur autre = utilisateur(9L, Role.ETUDIANT);

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(StageBusinessException.class,
                () -> service.signerEtudiant(30L, new SignerStageRequest("data"), autre)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(StageBusinessException.class,
                () -> service.signerEtudiant(30L, new SignerStageRequest("data"), encadrant)).getStatus());
        verify(signatures, never()).store(any(), any(), any());
    }

    @Test
    void refuseLaSignatureDUneEntrepriseQuiNePossedePasLOffre() {
        Entreprise autreSociete = new Entreprise();
        autreSociete.setId(99L);
        Utilisateur intrus = utilisateur(8L, Role.ENTREPRISE);
        intrus.setEntreprise(autreSociete);

        StageBusinessException erreur = assertThrows(StageBusinessException.class,
                () -> service.signerEncadrant(30L, new SignerStageRequest("data"), intrus));
        assertEquals(HttpStatus.FORBIDDEN, erreur.getStatus());
    }

    @Test
    void unCollegueNePeutPasSignerALaPlaceDeLEncadrantDesigne() {
        stage.setEncadrant(encadrant);
        Utilisateur collegue = utilisateur(4L, Role.ENTREPRISE);
        collegue.setEntreprise(encadrant.getEntreprise());

        StageBusinessException erreur = assertThrows(StageBusinessException.class,
                () -> service.signerEncadrant(30L, new SignerStageRequest("data"), collegue));
        assertEquals(HttpStatus.FORBIDDEN, erreur.getStatus());
    }

    @Test
    void refuseDeSignerUnStageDejaClotureOuNonAccepte() {
        stage.setStatutStage(StatutStage.CLOTURE);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(StageBusinessException.class,
                () -> service.signerEtudiant(30L, new SignerStageRequest("data"), etudiant)).getStatus());

        stage.setStatutStage(StatutStage.ACTIF);
        stage.setStatut(Candidature.StatutCandidature.EN_ATTENTE);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(StageBusinessException.class,
                () -> service.signerEtudiant(30L, new SignerStageRequest("data"), etudiant)).getStatus());
    }

    @Test
    void renvoie404PourUnStageInexistant() {
        when(candidatures.findById(77L)).thenReturn(Optional.empty());

        StageBusinessException erreur = assertThrows(StageBusinessException.class,
                () -> service.etatCloture(77L, etudiant));
        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatus());
    }

    @Test
    void lEtatDeClotureIndiqueQuiPeutSigner() {
        assertTrue(service.etatCloture(30L, etudiant).peutSignerEtudiant());
        assertTrue(service.etatCloture(30L, encadrant).peutSignerEncadrant());

        Utilisateur chef = utilisateur(5L, Role.CHEF_DEPT_STAGE);
        StageClotureResponseDTO vueChef = service.etatCloture(30L, chef);
        assertFalse(vueChef.peutSignerEtudiant());
        assertFalse(vueChef.peutSignerEncadrant());
    }

    @Test
    void refuseLaLectureAUnRoleNonAutorise() {
        Utilisateur admin = utilisateur(6L, Role.SUPER_ADMIN);

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(StageBusinessException.class,
                () -> service.etatCloture(30L, admin)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(StageBusinessException.class,
                () -> service.etatCloture(30L, null)).getStatus());
    }

    @Test
    void lesEcheancesDuMoisSontConvertiesPourLeCalendrier() {
        TacheAssignee tache = new TacheAssignee();
        tache.setId(50L);
        tache.setStage(stage);
        tache.setTitre("Rapport");
        tache.setDateEcheance(LocalDate.of(2026, 10, 20));
        tache.setStatut(StatutTacheAssignee.ASSIGNEE);
        etudiant.setPrenom("Lina");
        etudiant.setNom("Doe");
        when(taches.findEcheancesByStageId(30L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(tache));
        when(taches.findEcheancesByEtudiantId(2L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of());

        List<EcheanceCalendrierDTO> echeances = service.echeancesStage(30L, 2026, 10, etudiant);

        assertEquals(1, echeances.size());
        assertEquals("Lina Doe", echeances.get(0).nomEtudiant());
        assertEquals("Acme", echeances.get(0).entrepriseNom());
        assertEquals("Stage Java", echeances.get(0).offreTitre());
        assertTrue(service.echeancesEtudiant(2026, 2, etudiant).isEmpty());
        assertThrows(StageBusinessException.class, () -> service.echeancesEtudiant(2026, 2, encadrant));
    }

    @Test
    void leRapportPdfEstGenereAvecLesTachesDuStage() {
        List<TacheAssignee> liste = List.of(new TacheAssignee());
        when(taches.findByStageId(30L)).thenReturn(liste);
        when(rapports.generer(stage, liste)).thenReturn(new byte[]{1, 2, 3});

        assertArrayEquals(new byte[]{1, 2, 3}, service.telechargerRapport(30L, etudiant));
    }

    private Utilisateur utilisateur(Long id, Role role) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setRole(role);
        u.setPrenom("Prenom" + id);
        u.setNom("Nom" + id);
        u.setEmail("u" + id + "@example.com");
        return u;
    }
}
