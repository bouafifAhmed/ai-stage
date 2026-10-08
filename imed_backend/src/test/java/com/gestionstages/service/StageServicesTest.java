package com.gestionstages.service;

import com.gestionstages.dto.StageDTOs.CandidatureRequest;
import com.gestionstages.dto.StageDTOs.CandidatureStatutRequest;
import com.gestionstages.dto.StageDTOs.OffreRequest;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.*;
import com.gestionstages.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StageServicesTest {
    private OffreStageRepository offres;
    private CandidatureRepository candidatures;
    private UserRepository users;
    private CvStorageService cvStorage;
    private CvExtractionService cvExtraction;
    private NotificationEtudiantRepository notifications;
    private TacheAssigneeRepository tachesAssignees;
    private EntrepriseStageService entrepriseService;
    private EtudiantStageService etudiantService;

    @BeforeEach
    void setUp() {
        offres = mock(OffreStageRepository.class);
        candidatures = mock(CandidatureRepository.class);
        users = mock(UserRepository.class);
        cvStorage = mock(CvStorageService.class);
        cvExtraction = mock(CvExtractionService.class);
        notifications = mock(NotificationEtudiantRepository.class);
        tachesAssignees = mock(TacheAssigneeRepository.class);
        entrepriseService = new EntrepriseStageService(
                offres,
                candidatures,
                mock(EntrepriseRepository.class),
                users,
                notifications,
                tachesAssignees
        );
        etudiantService = new EtudiantStageService(
                offres,
                candidatures,
                users,
                cvStorage,
                cvExtraction,
                notifications,
                mock(QcmEtudiantService.class)
        );
    }

    @Test
    void refuseUneDoubleCandidature() {
        Utilisateur etudiant = etudiant();
        OffreStage offre = offrePubliee();
        when(offres.findByIdAndStatut(20L, OffreStage.StatutOffre.PUBLIEE))
                .thenReturn(Optional.of(offre));
        when(candidatures.existsByOffreIdAndEtudiantId(20L, 2L)).thenReturn(true);

        StageBusinessException exception = assertThrows(StageBusinessException.class,
                () -> etudiantService.postuler(etudiant, new CandidatureRequest(20L, "Motivé")));

        assertEquals(409, exception.getStatus().value());
        verify(candidatures, never()).save(any());
    }

    @Test
    void supprimeUneOffreAyantDesCandidatures() {
        Utilisateur compte = entreprise();
        OffreStage offre = offrePubliee();
        when(offres.findByIdAndEntrepriseId(20L, 10L)).thenReturn(Optional.of(offre));

        entrepriseService.supprimer(compte, 20L);

        verify(notifications).deleteByCandidatureOffreId(20L);
        verify(tachesAssignees).deleteByStageOffreId(20L);
        verify(candidatures).deleteByOffreId(20L);
        verify(offres).delete(offre);
        verify(offres, never()).save(any());
    }

    @Test
    void supprimePhysiquementUneOffreSansCandidature() {
        Utilisateur compte = entreprise();
        OffreStage offre = offrePubliee();
        when(offres.findByIdAndEntrepriseId(20L, 10L)).thenReturn(Optional.of(offre));

        entrepriseService.supprimer(compte, 20L);

        verify(offres).delete(offre);
        verify(offres, never()).save(any());
    }

    @Test
    void interditLaGestionAUneEntrepriseNonValidee() {
        Utilisateur compte = entreprise();
        compte.getEntreprise().setStatutValidation(StatutValidation.EN_ATTENTE);

        assertThrows(StageBusinessException.class,
                () -> entrepriseService.creer(compte, requeteOffre()));
        verify(offres, never()).save(any());
    }

    @Test
    void accepteUneCandidatureDeLEntreprise() {
        Utilisateur compte = entreprise();
        Candidature candidature = new Candidature();
        candidature.setId(30L);
        candidature.setOffre(offrePubliee());
        candidature.setEtudiant(etudiant());
        candidature.setMessage("Motivé");
        candidature.setStatut(Candidature.StatutCandidature.EN_ATTENTE);
        when(candidatures.findByIdAndOffreEntrepriseId(30L, 10L))
                .thenReturn(Optional.of(candidature));
        when(offres.findByIdAndEntrepriseIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(candidature.getOffre()));
        when(candidatures.save(candidature)).thenReturn(candidature);

        entrepriseService.modifierStatutCandidature(
                compte,
                30L,
                new CandidatureStatutRequest(Candidature.StatutCandidature.ACCEPTEE)
        );

        assertEquals(Candidature.StatutCandidature.ACCEPTEE, candidature.getStatut());
        verify(candidatures).save(candidature);
        verify(notifications).save(argThat(notification ->
                !notification.isLue()
                        && notification.getEtudiant().getId().equals(2L)
                        && notification.getMessage().contains("acceptée")
        ));
    }

    @Test
    void refuseUneAcceptationQuandToutesLesPlacesSontAttribuees() {
        Utilisateur compte = entreprise();
        Candidature candidature = new Candidature();
        candidature.setId(30L);
        candidature.setOffre(offrePubliee());
        candidature.setEtudiant(etudiant());
        candidature.setMessage("Motivé");
        candidature.setStatut(Candidature.StatutCandidature.EN_ATTENTE);
        when(candidatures.findByIdAndOffreEntrepriseId(30L, 10L))
                .thenReturn(Optional.of(candidature));
        when(offres.findByIdAndEntrepriseIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(candidature.getOffre()));
        when(candidatures.countByOffreIdAndStatut(
                20L,
                Candidature.StatutCandidature.ACCEPTEE
        )).thenReturn(1L);

        StageBusinessException exception = assertThrows(
                StageBusinessException.class,
                () -> entrepriseService.modifierStatutCandidature(
                        compte,
                        30L,
                        new CandidatureStatutRequest(Candidature.StatutCandidature.ACCEPTEE)
                )
        );

        assertEquals(409, exception.getStatus().value());
        assertEquals(Candidature.StatutCandidature.EN_ATTENTE, candidature.getStatut());
        verify(candidatures, never()).save(any());
        verify(notifications, never()).save(any());
    }

    @Test
    void refuseDeReduireLesPlacesSousLeNombreDeCandidaturesAcceptees() {
        Utilisateur compte = entreprise();
        OffreStage offre = offrePubliee();
        when(offres.findByIdAndEntrepriseIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(offre));
        when(candidatures.countByOffreIdAndStatut(
                20L,
                Candidature.StatutCandidature.ACCEPTEE
        )).thenReturn(2L);

        StageBusinessException exception = assertThrows(
                StageBusinessException.class,
                () -> entrepriseService.modifier(compte, 20L, requeteOffre())
        );

        assertEquals(400, exception.getStatus().value());
        verify(offres, never()).save(any());
    }

    @Test
    void compteLesNotificationsNonLuesDeLEtudiant() {
        Utilisateur etudiant = etudiant();
        when(notifications.countByEtudiantIdAndLueFalse(2L)).thenReturn(2L);

        long count = etudiantService.notificationsNonLues(etudiant).nonLues();

        assertEquals(2L, count);
    }

    private Utilisateur etudiant() {
        Utilisateur u = new Utilisateur();
        u.setId(2L); u.setRole(Role.ETUDIANT); u.setActif(true);
        u.setNom("Doe"); u.setPrenom("Lina"); u.setEmail("lina@example.com");
        return u;
    }

    private Utilisateur entreprise() {
        Entreprise e = entrepriseEntity();
        Utilisateur u = new Utilisateur();
        u.setId(3L); u.setRole(Role.ENTREPRISE); u.setActif(true); u.setEntreprise(e);
        return u;
    }

    private Entreprise entrepriseEntity() {
        Entreprise e = new Entreprise();
        e.setId(10L); e.setNom("Acme"); e.setEmailContact("contact@acme.tn");
        e.setStatutValidation(StatutValidation.VALIDEE);
        return e;
    }

    private OffreStage offrePubliee() {
        OffreStage o = new OffreStage();
        o.setId(20L); o.setTitre("Stage Java"); o.setDescription("Description");
        o.setDomaine("IT"); o.setLocalisation("Tunis"); o.setMode(OffreStage.ModeTravail.HYBRIDE);
        o.setTypeStage("PFE"); o.setDateLimite(LocalDate.now().plusDays(10));
        o.setCompetences(List.of("Java")); o.setNombrePlaces(1);
        o.setStatut(OffreStage.StatutOffre.PUBLIEE); o.setEntreprise(entrepriseEntity());
        return o;
    }

    private OffreRequest requeteOffre() {
        return new OffreRequest("Stage Java", "Description", "IT", "Tunis",
                OffreStage.ModeTravail.HYBRIDE, "PFE", 6, null, null,
                LocalDate.now().plusDays(10), List.of("Java"), "Bac+5", 1,
                null, OffreStage.StatutOffre.BROUILLON);
    }
}
