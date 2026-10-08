package com.gestionstages.service;

import com.gestionstages.dto.CompleterTacheDTO;
import com.gestionstages.dto.CreateTacheAssigneeDTO;
import com.gestionstages.dto.ValiderTacheDTO;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.OffreStage;
import com.gestionstages.model.Role;
import com.gestionstages.model.StatutTacheAssignee;
import com.gestionstages.model.TacheAssignee;
import com.gestionstages.model.TypeNotification;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.CandidatureRepository;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.NotificationEtudiantRepository;
import com.gestionstages.repository.TacheAssigneeRepository;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TacheAssigneeServiceTest {
    private TacheAssigneeRepository taches;
    private CandidatureRepository candidatures;
    private EntrepriseRepository entreprises;
    private UserRepository utilisateurs;
    private NotificationEtudiantRepository notifications;
    private TacheAttachmentStorageService stockage;
    private TacheAssigneeService service;

    @BeforeEach
    void setUp() {
        taches = mock(TacheAssigneeRepository.class);
        candidatures = mock(CandidatureRepository.class);
        entreprises = mock(EntrepriseRepository.class);
        utilisateurs = mock(UserRepository.class);
        notifications = mock(NotificationEtudiantRepository.class);
        stockage = mock(TacheAttachmentStorageService.class);
        service = new TacheAssigneeService(
                taches,
                candidatures,
                entreprises,
                utilisateurs,
                stockage,
                notifications
        );
        when(taches.save(any(TacheAssignee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taches.saveAndFlush(any(TacheAssignee.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void unEncadrantNePeutPasAssignerSurLeStageDUneAutreEntreprise() {
        Candidature stage = stageAccepte();
        Utilisateur autreEntreprise = entreprise(31L, entrepriseEntity(99L));
        when(candidatures.findByIdForUpdate(100L)).thenReturn(Optional.of(stage));

        StageBusinessException exception = assertThrows(
                StageBusinessException.class,
                () -> service.assignerTache(
                        100L,
                        new CreateTacheAssigneeDTO("API", "Créer les endpoints", LocalDate.now().plusDays(5)),
                        autreEntreprise
                )
        );

        assertEquals(403, exception.getStatus().value());
        verify(taches, never()).save(any());
    }

    @Test
    void unEtudiantNePeutPasTerminerLaTacheDUnAutreEtudiant() {
        TacheAssignee tache = tache(StatutTacheAssignee.ASSIGNEE);
        when(taches.findById(200L)).thenReturn(Optional.of(tache));
        Utilisateur autreEtudiant = utilisateur(22L, Role.ETUDIANT);

        StageBusinessException exception = assertThrows(
                StageBusinessException.class,
                () -> service.marquerCommeTerminee(
                        100L,
                        200L,
                        new CompleterTacheDTO("Travail livré", null),
                        autreEtudiant
                )
        );

        assertEquals(403, exception.getStatus().value());
        verify(taches, never()).save(any());
    }

    @Test
    void impossibleDeValiderUneTacheNonTerminee() {
        TacheAssignee tache = tache(StatutTacheAssignee.ASSIGNEE);
        when(taches.findById(200L)).thenReturn(Optional.of(tache));

        StageBusinessException exception = assertThrows(
                StageBusinessException.class,
                () -> service.validerTache(
                        100L,
                        200L,
                        new ValiderTacheDTO("Bien"),
                        tache.getEncadrant()
                )
        );

        assertEquals(400, exception.getStatus().value());
        verify(taches, never()).save(any());
    }

    @Test
    void leRejetExigeUnCommentaireNonBlanc() {
        TacheAssignee tache = tache(StatutTacheAssignee.TERMINEE);
        when(taches.findById(200L)).thenReturn(Optional.of(tache));

        StageBusinessException exception = assertThrows(
                StageBusinessException.class,
                () -> service.rejeterTache(
                        100L,
                        200L,
                        new ValiderTacheDTO("   "),
                        tache.getEncadrant()
                )
        );

        assertEquals(400, exception.getStatus().value());
        verify(taches, never()).save(any());
    }

    @Test
    void unMembreDeLaMemeEntreprisePeutAssignerMemeSiUnAutreEstEncadrant() {
        Candidature stage = stageAccepte();
        Utilisateur encadrantInitial = entreprise(30L, stage.getOffre().getEntreprise());
        stage.setEncadrant(encadrantInitial);
        Utilisateur autreMembre = entreprise(31L, stage.getOffre().getEntreprise());
        when(candidatures.findByIdForUpdate(100L)).thenReturn(Optional.of(stage));

        service.assignerTache(
                100L,
                new CreateTacheAssigneeDTO("Tests", "Écrire les tests unitaires", LocalDate.now().plusDays(3)),
                autreMembre
        );

        assertSame(encadrantInitial, stage.getEncadrant());
        verify(taches).saveAndFlush(any(TacheAssignee.class));
        verify(notifications).save(any());
    }

    @Test
    void laPremiereAssignationAffecteLEncardantEtCreeLaTache() {
        Candidature stage = stageAccepte();
        Utilisateur encadrant = entreprise(30L, stage.getOffre().getEntreprise());
        when(candidatures.findByIdForUpdate(100L)).thenReturn(Optional.of(stage));

        service.assignerTache(
                100L,
                new CreateTacheAssigneeDTO(" API ", " Endpoints REST ", LocalDate.now().plusDays(5)),
                encadrant
        );

        assertSame(encadrant, stage.getEncadrant());
        verify(candidatures).save(stage);
        verify(taches).saveAndFlush(any(TacheAssignee.class));
        verify(notifications).save(argThat(notification ->
                notification.getType() == TypeNotification.TACHE_ASSIGNEE
                        && !notification.isLue()
                        && notification.getMessage().contains("API")
        ));
    }

    @Test
    void leRejetRemetLaTacheEnCoursEtEffaceLaDateValidation() {
        TacheAssignee tache = tache(StatutTacheAssignee.TERMINEE);
        tache.setDateValidation(LocalDateTime.now().minusHours(1));
        when(taches.findById(200L)).thenReturn(Optional.of(tache));

        service.rejeterTache(100L, 200L, new ValiderTacheDTO(" À corriger "), tache.getEncadrant());

        assertEquals(StatutTacheAssignee.EN_COURS, tache.getStatut());
        assertEquals("À corriger", tache.getCommentaireEncadrant());
        assertNull(tache.getDateValidation());
    }

    @Test
    void letudiantPeutResoumettreUneTacheRejeteeMiseEnCours() {
        TacheAssignee tache = tache(StatutTacheAssignee.EN_COURS);
        when(taches.findById(200L)).thenReturn(Optional.of(tache));

        service.marquerCommeTerminee(
                100L,
                200L,
                new CompleterTacheDTO("Version corrigée", null),
                tache.getStage().getEtudiant()
        );

        assertEquals(StatutTacheAssignee.TERMINEE, tache.getStatut());
        assertEquals("Version corrigée", tache.getCommentaireEtudiant());
        assertNotNull(tache.getDateCompletion());
    }

    private TacheAssignee tache(StatutTacheAssignee statut) {
        Candidature stage = stageAccepte();
        Utilisateur encadrant = entreprise(30L, stage.getOffre().getEntreprise());
        stage.setEncadrant(encadrant);
        TacheAssignee tache = new TacheAssignee();
        tache.setId(200L);
        tache.setStage(stage);
        tache.setEncadrant(encadrant);
        tache.setTitre("API");
        tache.setDescription("Créer les endpoints");
        tache.setStatut(statut);
        tache.setDateAssignation(LocalDateTime.now().minusDays(1));
        return tache;
    }

    private Candidature stageAccepte() {
        Entreprise entreprise = entrepriseEntity(10L);
        OffreStage offre = new OffreStage();
        offre.setId(50L);
        offre.setEntreprise(entreprise);
        Candidature stage = new Candidature();
        stage.setId(100L);
        stage.setOffre(offre);
        stage.setEtudiant(utilisateur(20L, Role.ETUDIANT));
        stage.setStatut(Candidature.StatutCandidature.ACCEPTEE);
        return stage;
    }

    private Utilisateur entreprise(Long id, Entreprise entreprise) {
        Utilisateur utilisateur = utilisateur(id, Role.ENTREPRISE);
        utilisateur.setEntreprise(entreprise);
        return utilisateur;
    }

    private Utilisateur utilisateur(Long id, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setRole(role);
        utilisateur.setPrenom("Lina");
        utilisateur.setNom("Doe");
        return utilisateur;
    }

    private Entreprise entrepriseEntity(Long id) {
        Entreprise entreprise = new Entreprise();
        entreprise.setId(id);
        entreprise.setNom("Acme");
        return entreprise;
    }
}
