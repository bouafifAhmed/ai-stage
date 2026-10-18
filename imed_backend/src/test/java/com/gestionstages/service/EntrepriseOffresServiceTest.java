package com.gestionstages.service;

import com.gestionstages.dto.StageDTOs.CandidatureResponse;
import com.gestionstages.dto.StageDTOs.OffreRequest;
import com.gestionstages.dto.StageDTOs.OffreResponse;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.*;
import com.gestionstages.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Gestion des offres et lecture des candidatures par une entreprise.
 */
class EntrepriseOffresServiceTest {
    private OffreStageRepository offres;
    private CandidatureRepository candidatures;
    private EntrepriseRepository entreprises;
    private UserRepository users;
    private EntrepriseStageService service;
    private Utilisateur compte;
    private Entreprise acme;

    @BeforeEach
    void setUp() {
        offres = mock(OffreStageRepository.class);
        candidatures = mock(CandidatureRepository.class);
        entreprises = mock(EntrepriseRepository.class);
        users = mock(UserRepository.class);
        service = new EntrepriseStageService(offres, candidatures, entreprises, users,
                mock(NotificationEtudiantRepository.class), mock(TacheAssigneeRepository.class));
        when(offres.save(any())).thenAnswer(inv -> inv.getArgument(0));

        acme = new Entreprise();
        acme.setId(10L);
        acme.setNom("Acme");
        acme.setEmailContact("contact@acme.tn");
        acme.setStatutValidation(StatutValidation.VALIDEE);
        compte = new Utilisateur();
        compte.setId(3L);
        compte.setRole(Role.ENTREPRISE);
        compte.setActif(true);
        compte.setEmail("contact@acme.tn");
        compte.setEntreprise(acme);
    }

    @Test
    void laCreationNettoieLesChampsEtRattacheLOffreALEntreprise() {
        OffreResponse offre = service.creer(compte, requete(OffreStage.StatutOffre.PUBLIEE, LocalDate.now().plusDays(10), 2));

        assertEquals("Stage Java", offre.titre());
        assertEquals("Tunis", offre.localisation());
        assertEquals(10L, offre.entrepriseId());
        assertEquals(2, offre.placesRestantes());
    }

    @Test
    void desDatesIncoherentesSontRefusees() {
        OffreRequest finAvantDebut = new OffreRequest("Stage", "Desc", "IT", "Tunis", OffreStage.ModeTravail.HYBRIDE,
                "PFE", 6, LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(1), LocalDate.now().plusDays(5),
                List.of("Java"), "M1", 1, BigDecimal.ZERO, OffreStage.StatutOffre.BROUILLON);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(StageBusinessException.class,
                () -> service.creer(compte, finAvantDebut)).getStatus());

        assertThrows(StageBusinessException.class,
                () -> service.creer(compte, requete(OffreStage.StatutOffre.PUBLIEE, LocalDate.now().minusDays(1), 1)));
        verify(offres, never()).save(any());
    }

    @Test
    void unBrouillonPeutAvoirUneDateLimitePassee() {
        assertDoesNotThrow(() -> service.creer(compte, requete(OffreStage.StatutOffre.BROUILLON, LocalDate.now().minusDays(1), 1)));
    }

    @Test
    void uneEntrepriseNonValideeOuUnCompteInactifNePeutPasGererDesOffres() {
        acme.setStatutValidation(StatutValidation.EN_ATTENTE);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(StageBusinessException.class,
                () -> service.lister(compte, PageRequest.of(0, 5))).getStatus());

        acme.setStatutValidation(StatutValidation.VALIDEE);
        compte.setActif(false);
        assertThrows(StageBusinessException.class, () -> service.lister(compte, PageRequest.of(0, 5)));

        Utilisateur etudiant = new Utilisateur();
        etudiant.setRole(Role.ETUDIANT);
        assertThrows(StageBusinessException.class, () -> service.lister(etudiant, PageRequest.of(0, 5)));
    }

    @Test
    void unCompteSansLienRetrouveSonEntrepriseParLEmailDeContact() {
        compte.setEntreprise(null);
        when(entreprises.findByEmailContact("contact@acme.tn")).thenReturn(Optional.of(acme));
        PageRequest page = PageRequest.of(0, 5);
        when(offres.findAllByEntrepriseId(10L, page)).thenReturn(new PageImpl<>(List.of()));

        service.lister(compte, page);

        assertSame(acme, compte.getEntreprise());
        verify(users).save(compte);
    }

    @Test
    void sansEntrepriseAssocieeLAccesEstRefuse() {
        compte.setEntreprise(null);
        when(entreprises.findByEmailContact("contact@acme.tn")).thenReturn(Optional.empty());

        StageBusinessException erreur = assertThrows(StageBusinessException.class,
                () -> service.lister(compte, PageRequest.of(0, 5)));
        assertTrue(erreur.getMessage().contains("Aucune entreprise"));
    }

    @Test
    void uneEntrepriseNeVoitPasLOffreNiLesCandidaturesDUneAutre() {
        when(offres.findByIdAndEntrepriseId(20L, 10L)).thenReturn(Optional.empty());
        when(candidatures.findByIdAndOffreEntrepriseId(30L, 10L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(StageBusinessException.class,
                () -> service.detail(compte, 20L)).getStatus());
        assertThrows(StageBusinessException.class, () -> service.candidatures(compte, 20L, PageRequest.of(0, 5)));
        assertThrows(StageBusinessException.class, () -> service.candidature(compte, 30L));
    }

    @Test
    void lesCandidaturesDUneOffreSontListees() {
        OffreStage offre = offre();
        Candidature candidature = new Candidature();
        candidature.setId(30L);
        candidature.setOffre(offre);
        Utilisateur etudiant = new Utilisateur();
        etudiant.setId(2L);
        etudiant.setNom("Doe");
        candidature.setEtudiant(etudiant);
        PageRequest page = PageRequest.of(0, 5);
        when(offres.findByIdAndEntrepriseId(20L, 10L)).thenReturn(Optional.of(offre));
        when(candidatures.findAllByOffreIdAndOffreEntrepriseId(20L, 10L, page)).thenReturn(new PageImpl<>(List.of(candidature)));
        when(candidatures.findByIdAndOffreEntrepriseId(30L, 10L)).thenReturn(Optional.of(candidature));

        assertEquals(1, service.candidatures(compte, 20L, page).getTotalElements());
        CandidatureResponse detail = service.candidature(compte, 30L);
        assertEquals("Doe", detail.etudiantNom());
        assertEquals("Acme", detail.entrepriseNom());
    }

    private OffreRequest requete(OffreStage.StatutOffre statut, LocalDate dateLimite, int places) {
        return new OffreRequest(" Stage Java ", " Description ", " IT ", " Tunis ", OffreStage.ModeTravail.HYBRIDE,
                " PFE ", 6, null, null, dateLimite, List.of("Java"), " M1 ", places, BigDecimal.TEN, statut);
    }

    private OffreStage offre() {
        OffreStage offre = new OffreStage();
        offre.setId(20L);
        offre.setTitre("Stage Java");
        offre.setEntreprise(acme);
        offre.setNombrePlaces(1);
        offre.setCompetences(List.of("Java"));
        return offre;
    }
}
