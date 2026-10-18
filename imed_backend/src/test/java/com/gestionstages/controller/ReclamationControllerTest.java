package com.gestionstages.controller;

import com.gestionstages.dto.*;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.Role;
import com.gestionstages.model.StatutReclamation;
import com.gestionstages.model.TypeReclamation;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.ReclamationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Traduction des erreurs du service de réclamations en codes HTTP.
 */
class ReclamationControllerTest {
    private ReclamationService service;
    private ReclamationController controller;
    private Utilisateur etudiant;
    private ReclamationResponseDTO reclamation;

    @BeforeEach
    void setUp() {
        service = mock(ReclamationService.class);
        controller = new ReclamationController(service);
        etudiant = new Utilisateur();
        etudiant.setId(2L);
        etudiant.setRole(Role.ETUDIANT);
        reclamation = new ReclamationResponseDTO(40L, TypeReclamation.NOTE, "Note", StatutReclamation.OUVERTE,
                LocalDateTime.now(), null, "Lina Doe", null);
    }

    @Test
    void sansUtilisateurChaqueEndpointRepond401() {
        assertEquals(401, controller.creerReclamation(30L, new CreateReclamationDTO(), null).getStatusCode().value());
        assertEquals(401, controller.listerReclamationsParStage(30L, null).getStatusCode().value());
        assertEquals(401, controller.listerReclamationsEntreprise(null).getStatusCode().value());
        assertEquals(401, controller.getDetailReclamation(40L, null).getStatusCode().value());
        assertEquals(401, controller.ajouterMessage(40L, new CreateMessageDTO(), null).getStatusCode().value());
        assertEquals(401, controller.resoudreReclamation(40L, null).getStatusCode().value());
        assertEquals(401, controller.cloturerReclamation(40L, null).getStatusCode().value());
        assertEquals(401, controller.rouvrirReclamation(40L, null).getStatusCode().value());
        verifyNoInteractions(service);
    }

    @Test
    void laCreationRenvoie201OuLeCodeDeLErreurMetier() {
        CreateReclamationDTO dto = new CreateReclamationDTO();
        when(service.creerReclamation(30L, dto, 2L)).thenReturn(reclamation);
        assertEquals(201, controller.creerReclamation(30L, dto, etudiant).getStatusCode().value());

        when(service.creerReclamation(31L, dto, 2L)).thenThrow(new SecurityException("Pas votre stage"));
        ResponseEntity<?> interdit = controller.creerReclamation(31L, dto, etudiant);
        assertEquals(403, interdit.getStatusCode().value());
        assertEquals(Map.of("message", "Pas votre stage"), interdit.getBody());

        when(service.creerReclamation(32L, dto, 2L)).thenThrow(new IllegalArgumentException("Stage introuvable"));
        assertEquals(400, controller.creerReclamation(32L, dto, etudiant).getStatusCode().value());
    }

    @Test
    void uneErreurInattendueDonne500SansDetailTechnique() {
        CreateReclamationDTO dto = new CreateReclamationDTO();
        when(service.creerReclamation(30L, dto, 2L)).thenThrow(new IllegalStateException("NullPointer dans le mapping"));

        ResponseEntity<?> reponse = controller.creerReclamation(30L, dto, etudiant);

        assertEquals(500, reponse.getStatusCode().value());
        assertFalse(reponse.getBody().toString().contains("NullPointer"));
    }

    @Test
    void lEntrepriseDoitEtreRattacheeAUneSocietePourListerSesReclamations() {
        Utilisateur compte = new Utilisateur();
        compte.setRole(Role.ENTREPRISE);
        assertEquals(400, controller.listerReclamationsEntreprise(compte).getStatusCode().value());

        Entreprise acme = new Entreprise();
        acme.setId(10L);
        compte.setEntreprise(acme);
        when(service.listerReclamationsParEntreprise(10L)).thenReturn(List.of(reclamation));
        ResponseEntity<?> reponse = controller.listerReclamationsEntreprise(compte);
        assertEquals(200, reponse.getStatusCode().value());
        assertEquals(List.of(reclamation), reponse.getBody());
    }

    @Test
    void unDetailIntrouvableDonne404() {
        when(service.getDetailReclamation(99L)).thenThrow(new IllegalArgumentException("Réclamation introuvable"));

        assertEquals(404, controller.getDetailReclamation(99L, etudiant).getStatusCode().value());
    }

    @Test
    void leMessageTransmetLeRoleDeLAuteur() {
        CreateMessageDTO dto = new CreateMessageDTO();
        when(service.ajouterMessage(eq(40L), eq(dto), eq(2L), eq("ETUDIANT"))).thenReturn(new MessageResponseDTO());

        assertEquals(201, controller.ajouterMessage(40L, dto, etudiant).getStatusCode().value());

        when(service.ajouterMessage(eq(41L), any(), any(), any())).thenThrow(new IllegalArgumentException("Clôturée"));
        assertEquals(400, controller.ajouterMessage(41L, dto, etudiant).getStatusCode().value());
    }

    @Test
    void lesTransitionsDeStatutRenvoientLesBonsCodes() {
        when(service.resoudreReclamation(40L, 2L)).thenReturn(reclamation);
        when(service.cloturerReclamation(40L, 2L)).thenThrow(new SecurityException("Pas en charge"));
        when(service.rouvrirReclamation(40L, 2L)).thenThrow(new IllegalArgumentException("Délai dépassé"));

        assertEquals(200, controller.resoudreReclamation(40L, etudiant).getStatusCode().value());
        assertEquals(403, controller.cloturerReclamation(40L, etudiant).getStatusCode().value());
        assertEquals(400, controller.rouvrirReclamation(40L, etudiant).getStatusCode().value());
    }
}
