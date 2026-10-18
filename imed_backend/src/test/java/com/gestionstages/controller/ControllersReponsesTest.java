package com.gestionstages.controller;

import com.gestionstages.dto.*;
import com.gestionstages.dto.QcmDTOs.SoumettreQcmRequest;
import com.gestionstages.dto.StageDTOs.CandidatureRequest;
import com.gestionstages.dto.StageDTOs.OffreRequest;
import com.gestionstages.dto.StageSuiviDTOs.SignerStageRequest;
import com.gestionstages.model.StatutTacheAssignee;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Contrôleurs « minces » : codes HTTP, en-têtes de téléchargement et délégation au service.
 */
class ControllersReponsesTest {
    private final Utilisateur user = new Utilisateur();

    @Test
    void espaceEtudiant() {
        EtudiantStageService service = mock(EtudiantStageService.class);
        StageSuiviService suivi = mock(StageSuiviService.class);
        EtudiantStageController controller = new EtudiantStageController(service, suivi);
        when(service.telechargerCv(user)).thenReturn(
                new EtudiantStageService.CvDocument(new ByteArrayResource(new byte[]{1}), "mon cv.pdf"));

        ResponseEntity<?> cv = controller.telechargerCv(user);
        assertEquals("application/pdf", cv.getHeaders().getContentType().toString());
        assertTrue(cv.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment"));
        assertTrue(cv.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("cv.pdf"));

        assertEquals(201, controller.postuler(user, new CandidatureRequest(20L, "Motivée")).getStatusCode().value());
        controller.supprimerCv(user);
        verify(service).supprimerCv(user);

        controller.echeances(user, 2026, 10);
        verify(suivi).echeancesEtudiant(2026, 10, user);
        controller.marquerNotificationLue(user, 60L);
        verify(service).marquerNotificationLue(user, 60L);
        controller.offres(user, PageRequest.of(0, 5));
        controller.profil(user);
        controller.notificationsNonLues(user);
        verify(service).offres(user, PageRequest.of(0, 5));
        verify(service).profil(user);
        verify(service).notificationsNonLues(user);
    }

    @Test
    void espaceEntreprise() {
        EntrepriseStageService service = mock(EntrepriseStageService.class);
        EntrepriseOffreController controller = new EntrepriseOffreController(service);
        OffreRequest requete = mock(OffreRequest.class);

        assertEquals(201, controller.creer(user, requete).getStatusCode().value());
        assertEquals(204, controller.supprimer(user, 20L).getStatusCode().value());
        verify(service).supprimer(user, 20L);
        controller.candidatures(user, 20L, PageRequest.of(0, 5));
        verify(service).candidatures(user, 20L, PageRequest.of(0, 5));
    }

    @Test
    void suiviDeStage() {
        StageSuiviService service = mock(StageSuiviService.class);
        StageSuiviController controller = new StageSuiviController(service);
        when(service.telechargerRapport(30L, user)).thenReturn(new byte[]{37, 80, 68, 70});

        ResponseEntity<byte[]> rapport = controller.rapportPdf(30L, user);

        assertEquals("application/pdf", rapport.getHeaders().getContentType().toString());
        assertTrue(rapport.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("rapport-stage-30.pdf"));
        assertArrayEquals(new byte[]{37, 80, 68, 70}, rapport.getBody());

        SignerStageRequest signature = new SignerStageRequest("data");
        controller.signerEtudiant(30L, signature, user);
        controller.signerEncadrant(30L, signature, user);
        verify(service).signerEtudiant(30L, signature, user);
        verify(service).signerEncadrant(30L, signature, user);
    }

    @Test
    void tachesAssignees() {
        TacheAssigneeService service = mock(TacheAssigneeService.class);
        TacheAssigneeController controller = new TacheAssigneeController(service);
        when(service.telechargerPieceJointe(30L, 50L, user)).thenReturn(new TacheAttachmentStorageService.AttachmentDocument(
                new ByteArrayResource(new byte[]{1}), "maquette.png", "image/png"));

        ResponseEntity<?> piece = controller.telechargerPieceJointe(30L, 50L, user);
        assertEquals("image/png", piece.getHeaders().getContentType().toString());
        assertTrue(piece.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("maquette.png"));

        assertEquals(201, controller.assigner(30L, mock(CreateTacheAssigneeDTO.class), user).getStatusCode().value());
        MockMultipartFile fichier = new MockMultipartFile("pieceJointe", "rendu.pdf", "application/pdf", new byte[]{1});
        controller.terminer(30L, 50L, "Fait", fichier, user);
        verify(service).marquerCommeTerminee(eq(30L), eq(50L), any(), eq(fichier), eq(user));
        controller.lister(30L, StatutTacheAssignee.TERMINEE, user);
        verify(service).listerTachesParStage(30L, StatutTacheAssignee.TERMINEE, user);
    }

    @Test
    void qcmEtAuthentification() {
        QcmEtudiantService qcm = mock(QcmEtudiantService.class);
        assertEquals(201, new QcmEtudiantController(qcm)
                .soumettre(user, mock(SoumettreQcmRequest.class)).getStatusCode().value());

        AuthService auth = mock(AuthService.class);
        AuthController controller = new AuthController(auth);
        assertEquals(201, controller.register(mock(RegisterRequestDTO.class)).getStatusCode().value());
        assertEquals(201, controller.registerEntreprise(mock(CreateEntrepriseDTO.class)).getStatusCode().value());
        assertEquals(200, controller.login(mock(LoginRequestDTO.class)).getStatusCode().value());
    }

    @Test
    void administration() {
        EntrepriseService entreprises = mock(EntrepriseService.class);
        AdminEntrepriseController adminEntreprises = new AdminEntrepriseController(entreprises);
        assertEquals(201, adminEntreprises.creerEntreprise(mock(CreateEntrepriseDTO.class)).getStatusCode().value());
        assertEquals(204, adminEntreprises.desactiverEntreprise(10L).getStatusCode().value());
        assertEquals(200, adminEntreprises.validerEntreprise(10L).getStatusCode().value());
        verify(entreprises).desactiverEntreprise(10L);
        verify(entreprises).validerEntreprise(10L);

        AdminEtudiantService etudiants = mock(AdminEtudiantService.class);
        AdminEtudiantController adminEtudiants = new AdminEtudiantController(etudiants);
        assertEquals(201, adminEtudiants.creerEtudiant(mock(CreateEtudiantDTO.class)).getStatusCode().value());
        assertEquals(204, adminEtudiants.desactiverEtudiant(2L).getStatusCode().value());
        assertEquals(200, adminEtudiants.listerEtudiants(true, "info", PageRequest.of(0, 5)).getStatusCode().value());
        verify(etudiants).listerEtudiants(true, "info", PageRequest.of(0, 5));
    }
}
