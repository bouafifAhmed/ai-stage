package com.gestionstages.service;

import com.gestionstages.dto.recommandation.*;
import com.gestionstages.model.*;
import com.gestionstages.repository.CandidatureRepository;
import com.gestionstages.repository.OffreStageRepository;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecommandationClientServiceTest {
    private RestTemplate restTemplate;
    private UserRepository users;
    private OffreStageRepository offres;
    private RecommandationClientService service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        users = mock(UserRepository.class);
        offres = mock(OffreStageRepository.class);
        service = new RecommandationClientService(restTemplate, users, mock(CandidatureRepository.class), offres);
        ReflectionTestUtils.setField(service, "recommandationServiceUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(service, "recommanderEndpoint", "/recommander");
        ReflectionTestUtils.setField(service, "healthEndpoint", "/health");
        ReflectionTestUtils.setField(service, "adequationEndpoint", "/analyser-adequation");
    }

    @Test
    void construitLeProfilEtLesOffresPublieesAvecLesValeursParDefaut() {
        when(users.findById(2L)).thenReturn(Optional.of(etudiantSansProfil()));
        OffreStage offre = offrePubliee();
        offre.setEntreprise(null);
        offre.setDomaine(" ");
        when(offres.findAllByStatut(OffreStage.StatutOffre.PUBLIEE)).thenReturn(List.of(offre));
        when(restTemplate.postForObject(anyString(), any(), eq(RecommandationResponseDTO.class)))
                .thenReturn(new RecommandationResponseDTO(List.of(new RecommandationItemDTO(20L, "Entreprise", 0.4))));

        List<RecommandationItemDTO> recommandations = service.obtenirRecommandations(2L);

        ArgumentCaptor<RecommandationRequestDTO> requete = ArgumentCaptor.forClass(RecommandationRequestDTO.class);
        verify(restTemplate).postForObject(eq("http://localhost:8000/recommander"), requete.capture(),
                eq(RecommandationResponseDTO.class));
        EtudiantProfilDTO etudiant = requete.getValue().getEtudiant();
        assertEquals("Général", etudiant.getFiliere());
        assertEquals("L3", etudiant.getNiveau());
        assertEquals("Général", etudiant.getMotsClesSujetSouhaite());
        OffreEntrepriseDTO dto = requete.getValue().getOffres().get(0);
        assertEquals("Entreprise", dto.getNomEntreprise());
        assertEquals("Général", dto.getSecteurActivite());
        assertEquals("Stage Java Développement d'une API Spring", dto.getSujetOffre());
        assertEquals(1, recommandations.size());
        verify(offres, never()).findAll();
    }

    @Test
    void renvoieUneListeVideSansOffrePubliee() {
        when(users.findById(2L)).thenReturn(Optional.of(etudiantSansProfil()));
        when(offres.findAllByStatut(OffreStage.StatutOffre.PUBLIEE)).thenReturn(List.of());

        assertTrue(service.obtenirRecommandations(2L).isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void renvoieUneListeVideQuandLeServicePythonEstInjoignable() {
        when(users.findById(2L)).thenReturn(Optional.of(etudiantSansProfil()));
        when(offres.findAllByStatut(OffreStage.StatutOffre.PUBLIEE)).thenReturn(List.of(offrePubliee()));
        when(restTemplate.postForObject(anyString(), any(), eq(RecommandationResponseDTO.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertTrue(service.obtenirRecommandations(2L).isEmpty());
    }

    @Test
    void calculeLAdequationLocalementQuandLeServicePythonEstInjoignable() {
        Utilisateur etudiant = etudiantSansProfil();
        etudiant.setCompetences(List.of("JavaScript", "Spring Boot"));
        when(users.findById(2L)).thenReturn(Optional.of(etudiant));
        OffreStage offre = offrePubliee();
        offre.setCompetences(List.of("Java", "Spring"));
        when(offres.findByIdAndStatut(20L, OffreStage.StatutOffre.PUBLIEE)).thenReturn(Optional.of(offre));
        when(restTemplate.postForObject(anyString(), any(), eq(AdequationResponseDTO.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        AdequationResponseDTO adequation = service.analyserAdequation(2L, 20L);

        assertEquals("regles-spring", adequation.getSource());
        assertEquals(List.of("Spring"), adequation.getCompetencesAcquises());
        assertEquals(List.of("Java"), adequation.getCompetencesManquantes());
        assertEquals(50, adequation.getScorePourcentage());
        assertEquals(2, adequation.getCorrespondances().size());
    }

    @Test
    void refuseLAdequationPourUneOffreNonPubliee() {
        when(users.findById(2L)).thenReturn(Optional.of(etudiantSansProfil()));
        when(offres.findByIdAndStatut(20L, OffreStage.StatutOffre.PUBLIEE)).thenReturn(Optional.empty());

        assertThrows(RecommandationClientService.ResourceNotFoundException.class,
                () -> service.analyserAdequation(2L, 20L));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void compareLesCompetencesMotAMot() {
        assertFalse(RecommandationClientService.competencesCorrespondent("Java", "JavaScript"));
        assertFalse(RecommandationClientService.competencesCorrespondent("R", "React"));
        assertFalse(RecommandationClientService.competencesCorrespondent("C", "C++"));
        assertTrue(RecommandationClientService.competencesCorrespondent("Sécurité", "securite"));
        assertTrue(RecommandationClientService.competencesCorrespondent("Python", "Python 3"));
    }

    private Utilisateur etudiantSansProfil() {
        Utilisateur u = new Utilisateur();
        u.setId(2L); u.setRole(Role.ETUDIANT); u.setActif(true);
        u.setNom("Doe"); u.setPrenom("Lina"); u.setEmail("lina@example.com");
        return u;
    }

    private OffreStage offrePubliee() {
        Entreprise e = new Entreprise();
        e.setId(10L); e.setNom("Acme");
        OffreStage o = new OffreStage();
        o.setId(20L); o.setTitre("Stage Java"); o.setDescription("Développement d'une API Spring");
        o.setDomaine("IT"); o.setCompetences(List.of("Java"));
        o.setStatut(OffreStage.StatutOffre.PUBLIEE); o.setEntreprise(e);
        return o;
    }
}
