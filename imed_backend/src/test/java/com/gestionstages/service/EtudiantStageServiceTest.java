package com.gestionstages.service;

import com.gestionstages.dto.StageDTOs.*;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.*;
import com.gestionstages.repository.CandidatureRepository;
import com.gestionstages.repository.NotificationEtudiantRepository;
import com.gestionstages.repository.OffreStageRepository;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EtudiantStageServiceTest {
    private OffreStageRepository offres;
    private CandidatureRepository candidatures;
    private UserRepository users;
    private CvStorageService cvStorage;
    private CvExtractionService cvExtraction;
    private NotificationEtudiantRepository notifications;
    private QcmEtudiantService qcm;
    private EtudiantStageService service;
    private Utilisateur etudiant;

    @BeforeEach
    void setUp() {
        offres = mock(OffreStageRepository.class);
        candidatures = mock(CandidatureRepository.class);
        users = mock(UserRepository.class);
        cvStorage = mock(CvStorageService.class);
        cvExtraction = mock(CvExtractionService.class);
        notifications = mock(NotificationEtudiantRepository.class);
        qcm = mock(QcmEtudiantService.class);
        service = new EtudiantStageService(offres, candidatures, users, cvStorage, cvExtraction, notifications, qcm);

        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(candidatures.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notifications.save(any())).thenAnswer(inv -> inv.getArgument(0));

        etudiant = new Utilisateur();
        etudiant.setId(2L);
        etudiant.setRole(Role.ETUDIANT);
        etudiant.setNom("Doe");
        etudiant.setPrenom("Lina");
        etudiant.setEmail("lina@example.com");
    }

    @Test
    void lesOffresPublieesIndiquentLesPlacesRestantes() {
        OffreStage offre = offre(LocalDate.now().plusDays(5));
        PageRequest page = PageRequest.of(0, 10);
        when(offres.findAllByStatut(OffreStage.StatutOffre.PUBLIEE, page)).thenReturn(new PageImpl<>(List.of(offre)));
        when(candidatures.countByOffreIdAndStatut(20L, Candidature.StatutCandidature.ACCEPTEE)).thenReturn(1L);

        Page<OffreResponse> resultat = service.offres(etudiant, page);

        assertEquals(1, resultat.getTotalElements());
        assertEquals(1, resultat.getContent().get(0).placesOccupees());
        assertEquals(2, resultat.getContent().get(0).placesRestantes());
    }

    @Test
    void uneOffreNonPublieeRenvoie404() {
        when(offres.findByIdAndStatut(20L, OffreStage.StatutOffre.PUBLIEE)).thenReturn(Optional.empty());

        StageBusinessException erreur = assertThrows(StageBusinessException.class, () -> service.offre(etudiant, 20L));
        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatus());
    }

    @Test
    void lEspaceEtudiantEstInterditAuxAutresRoles() {
        Utilisateur entreprise = new Utilisateur();
        entreprise.setRole(Role.ENTREPRISE);

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(StageBusinessException.class,
                () -> service.profil(entreprise)).getStatus());
        assertThrows(StageBusinessException.class, () -> service.profil(null));
    }

    @Test
    void laModificationDuProfilNettoieLesChamps() {
        ProfilResponse profil = service.modifierProfil(etudiant,
                new ProfilRequest(" 71000000 ", " Informatique ", " M1 ", List.of("Java", "SQL")));

        assertEquals("71000000", profil.telephone());
        assertEquals("Informatique", profil.filiere());
        assertEquals("M1", profil.niveauEtudes());
        assertEquals(List.of("Java", "SQL"), profil.competences());
        assertFalse(profil.cvPresent());
    }

    @Test
    void leDepotDuCvEnregistreLeFichierEtRenvoieLExtraction() {
        MockMultipartFile cv = new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF".getBytes());
        CvExtractionResponse extraction = new CvExtractionResponse("71000000", "Informatique", "M1", List.of("Java"), true);
        when(cvStorage.safeOriginalFilename(cv)).thenReturn("cv.pdf");
        when(cvExtraction.extract(any(byte[].class))).thenReturn(extraction);

        CvUploadResponse reponse = service.enregistrerCv(etudiant, cv);

        verify(cvStorage).store(2L, cv);
        assertTrue(reponse.profil().cvPresent());
        assertEquals("cv.pdf", reponse.profil().cvNomFichier());
        assertNotNull(etudiant.getCvDateDepot());
        assertSame(extraction, reponse.extraction());
    }

    @Test
    void lExtractionEtLeTelechargementExigentUnCvDepose() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(StageBusinessException.class,
                () -> service.extraireCv(etudiant)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(StageBusinessException.class,
                () -> service.telechargerCv(etudiant)).getStatus());
    }

    @Test
    void lExtractionRelitLeCvStocke() {
        etudiant.setCvNomFichier("cv.pdf");
        when(cvStorage.load(2L)).thenReturn(new ByteArrayResource("%PDF".getBytes()));
        CvExtractionResponse extraction = CvExtractionResponse.empty();
        when(cvExtraction.extract(any(InputStream.class))).thenReturn(extraction);

        assertSame(extraction, service.extraireCv(etudiant));
        assertEquals("cv.pdf", service.telechargerCv(etudiant).filename());
    }

    @Test
    void unCvIlisibleDonneUneErreurServeurPropre() throws IOException {
        etudiant.setCvNomFichier("cv.pdf");
        Resource illisible = mock(Resource.class);
        when(illisible.getInputStream()).thenThrow(new IOException("disque"));
        when(cvStorage.load(2L)).thenReturn(illisible);

        StageBusinessException erreur = assertThrows(StageBusinessException.class, () -> service.extraireCv(etudiant));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, erreur.getStatus());
    }

    @Test
    void laSuppressionDuCvEffaceLeFichierEtLesMetadonnees() {
        etudiant.setCvNomFichier("cv.pdf");

        service.supprimerCv(etudiant);

        verify(cvStorage).delete(2L);
        assertNull(etudiant.getCvNomFichier());
        assertNull(etudiant.getCvDateDepot());
    }

    @Test
    void postulerCreeUneCandidatureEnAttente() {
        when(offres.findByIdAndStatut(20L, OffreStage.StatutOffre.PUBLIEE))
                .thenReturn(Optional.of(offre(LocalDate.now().plusDays(1))));

        CandidatureResponse reponse = service.postuler(etudiant, new CandidatureRequest(20L, "  Motivée  "));

        assertEquals(Candidature.StatutCandidature.EN_ATTENTE, reponse.statut());
        assertEquals("Motivée", reponse.message());
        verify(qcm).verifierAdmissibilite(etudiant);
    }

    @Test
    void postulerApresLaDateLimiteEstRefuse() {
        when(offres.findByIdAndStatut(20L, OffreStage.StatutOffre.PUBLIEE))
                .thenReturn(Optional.of(offre(LocalDate.now().minusDays(1))));

        StageBusinessException erreur = assertThrows(StageBusinessException.class,
                () -> service.postuler(etudiant, new CandidatureRequest(20L, "Motivée")));
        assertEquals(HttpStatus.CONFLICT, erreur.getStatus());
        verify(candidatures, never()).save(any());
    }

    @Test
    void postulerSansAvoirReussiLeQcmEstRefuse() {
        doThrow(new StageBusinessException(HttpStatus.FORBIDDEN, "QCM requis")).when(qcm).verifierAdmissibilite(etudiant);

        assertThrows(StageBusinessException.class,
                () -> service.postuler(etudiant, new CandidatureRequest(20L, "Motivée")));
        verifyNoInteractions(offres);
    }

    @Test
    void lesNotificationsSontListeesEtMarqueesCommeLues() {
        NotificationEtudiant notification = notification();
        PageRequest page = PageRequest.of(0, 5);
        when(notifications.findAllByEtudiantId(2L, page)).thenReturn(new PageImpl<>(List.of(notification)));
        when(notifications.countByEtudiantIdAndLueFalse(2L)).thenReturn(1L);
        when(notifications.findByIdAndEtudiantId(60L, 2L)).thenReturn(Optional.of(notification));

        assertEquals(1, service.notifications(etudiant, page).getTotalElements());
        assertEquals(1, service.notificationsNonLues(etudiant).nonLues());
        assertTrue(service.marquerNotificationLue(etudiant, 60L).lue());
    }

    @Test
    void onNePeutPasMarquerLaNotificationDUnAutreEtudiant() {
        when(notifications.findByIdAndEtudiantId(61L, 2L)).thenReturn(Optional.empty());

        StageBusinessException erreur = assertThrows(StageBusinessException.class,
                () -> service.marquerNotificationLue(etudiant, 61L));
        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatus());
        verify(notifications, never()).save(any());
    }

    @Test
    void lesCandidaturesDeLEtudiantSontPaginees() {
        Candidature candidature = new Candidature();
        candidature.setOffre(offre(LocalDate.now()));
        candidature.setEtudiant(etudiant);
        PageRequest page = PageRequest.of(0, 5);
        when(candidatures.findAllByEtudiantId(2L, page)).thenReturn(new PageImpl<>(List.of(candidature)));

        assertEquals("Stage Java", service.candidatures(etudiant, page).getContent().get(0).offreTitre());
    }

    private OffreStage offre(LocalDate dateLimite) {
        Entreprise acme = new Entreprise();
        acme.setId(10L);
        acme.setNom("Acme");
        OffreStage offre = new OffreStage();
        offre.setId(20L);
        offre.setTitre("Stage Java");
        offre.setEntreprise(acme);
        offre.setNombrePlaces(3);
        offre.setCompetences(List.of("Java"));
        offre.setDateLimite(dateLimite);
        offre.setStatut(OffreStage.StatutOffre.PUBLIEE);
        return offre;
    }

    private NotificationEtudiant notification() {
        Candidature candidature = new Candidature();
        candidature.setId(30L);
        candidature.setOffre(offre(LocalDate.now()));
        NotificationEtudiant notification = new NotificationEtudiant();
        notification.setId(60L);
        notification.setEtudiant(etudiant);
        notification.setCandidature(candidature);
        notification.setMessage("Candidature acceptée");
        return notification;
    }
}
