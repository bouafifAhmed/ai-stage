package com.gestionstages.service;

import com.gestionstages.dto.recommandation.*;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.CandidatureRepository;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service client pour communiquer avec le microservice Python de recommandation.
 *
 * Responsabilités :
 * 1. Récupérer le profil de l'utilisateur connecté depuis la base de données
 * 2. Récupérer toutes les offres/entreprises disponibles
 * 3. Transformer les entités Spring en DTOs Pydantic
 * 4. Envoyer une requête POST au service Python
 * 5. Gérer les erreurs gracieusement (si le service Python est indisponible)
 *
 * Architecture :
 * - Le service Python est STATELESS (aucune base de données)
 * - Spring Boot reste la source unique de vérité
 * - À chaque appel, on envoie l'utilisateur ET la liste complète des offres actuelles
 * - Le service Python calcule et retourne immédiatement les recommandations
 */
import com.gestionstages.model.OffreStage;
import com.gestionstages.repository.OffreStageRepository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class RecommandationClientService {

    @Value("${recommandation.service.url:http://localhost:8000}")
    private String recommandationServiceUrl;

    @Value("${recommandation.service.endpoint.recommander:/recommander}")
    private String recommanderEndpoint;

    @Value("${recommandation.service.endpoint.health:/health}")
    private String healthEndpoint;

    @Value("${recommandation.service.endpoint.adequation:/analyser-adequation}")
    private String adequationEndpoint;

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final CandidatureRepository candidatureRepository;
    private final OffreStageRepository offreStageRepository;

    public RecommandationClientService(
            RestTemplate restTemplate,
            UserRepository userRepository,
            EntrepriseRepository entrepriseRepository,
            CandidatureRepository candidatureRepository,
            OffreStageRepository offreStageRepository
    ) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.candidatureRepository = candidatureRepository;
        this.offreStageRepository = offreStageRepository;
    }

    /**
     * Vérifie que le service Python de recommandation est actif et accessible.
     *
     * @return true si le service répond avec 200 OK, false sinon
     */
    public boolean isServiceAvailable() {
        try {
            String healthUrl = recommandationServiceUrl + healthEndpoint;
            log.debug("Vérification de l'état du service Python : {}", healthUrl);

            Object response = restTemplate.getForObject(healthUrl, Object.class);
            log.info("Service de recommandation disponible");
            return true;
        } catch (RestClientException e) {
            log.warn("Service de recommandation indisponible : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtient les recommandations d'offres pour un utilisateur.
     *
     * Processus :
     * 1. Récupérer l'utilisateur (étudiant) depuis la base de données
     * 2. Récupérer toutes les entreprises disponibles
     * 3. Transformer en DTOs
     * 4. Envoyer au service Python via POST
     * 5. Retourner les recommandations triées par score
     *
     * Gestion d'erreur :
     * - Si le service Python est indisponible : retourner une liste vide (fonctionnalité non-critique)
     * - Si l'utilisateur n'existe pas : lever une exception (cas d'erreur applicatif)
     * - Si aucune offre disponible : retourner une liste vide
     *
     * @param etudiantId Identifiant de l'étudiant (utilisateur) connecté
     * @return List<RecommandationItemDTO> : offres recommandées triées par score décroissant
     * @throws ResourceNotFoundException si l'étudiant n'existe pas
     */
    public List<RecommandationItemDTO> obtenirRecommandations(Long etudiantId) {
        log.info("Demande de recommandations pour l'étudiant {}", etudiantId);

        try {
            // Étape 1 : Récupérer l'utilisateur (étudiant)
            Utilisateur utilisateur = userRepository.findById(etudiantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec id : " + etudiantId));

            // Étape 2 : Récupérer toutes les offres (priorité aux offres de stage)
            List<OffreStage> offresStage = offreStageRepository.findAll();
            List<OffreEntrepriseDTO> offresEntreprise;

            if (!offresStage.isEmpty()) {
                offresEntreprise = construireOffresDepuisOffreStage(offresStage);
            } else {
                // Fallback si aucune offre de stage n'a encore été publiée
                List<Entreprise> entreprises = entrepriseRepository.findAll();
                if (entreprises.isEmpty()) {
                    log.warn("Aucune offre ni entreprise disponible pour les recommandations");
                    return Collections.emptyList();
                }
                offresEntreprise = construireOffresEntreprise(entreprises);
            }

            if (offresEntreprise.isEmpty()) {
                log.warn("Aucune offre disponible pour les recommandations");
                return Collections.emptyList();
            }

            // Étape 3 : Transformer en DTOs
            EtudiantProfilDTO profilEtudiant = construireProfilEtudiant(utilisateur);

            // Étape 4 : Envoyer au service Python
            RecommandationRequestDTO requete = RecommandationRequestDTO.builder()
                    .etudiant(profilEtudiant)
                    .offres(offresEntreprise)
                    .build();

            log.debug("Envoi de la requête de recommandation au service Python : {} offres",
                    offresEntreprise.size());

            String url = recommandationServiceUrl + recommanderEndpoint;
            RecommandationResponseDTO reponse = restTemplate.postForObject(
                    url,
                    requete,
                    RecommandationResponseDTO.class
            );

            if (reponse == null || reponse.getRecommandations() == null) {
                log.warn("Réponse vide du service Python");
                return Collections.emptyList();
            }

            log.info("Recommandations reçues : {} offres pour l'étudiant {}",
                    reponse.getRecommandations().size(), etudiantId);

            return reponse.getRecommandations();

        } catch (ResourceNotFoundException e) {
            log.error("Erreur applicative : {}", e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.error("Erreur lors de la communication avec le service Python : {}", e.getMessage());
            log.warn("Service de recommandation indisponible. Retour d'une liste vide (fonctionnalité optionnelle)");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Erreur inattendue lors du calcul des recommandations", e);
            return Collections.emptyList();
        }
    }

    /**
     * Construit le DTO du profil utilisateur à partir de l'entité JPA.
     */
    private EtudiantProfilDTO construireProfilEtudiant(Utilisateur utilisateur) {
        String filiere = (utilisateur.getFiliere() != null && !utilisateur.getFiliere().isBlank())
                ? utilisateur.getFiliere() : "Général";
        String niveau = (utilisateur.getNiveauEtudes() != null && !utilisateur.getNiveauEtudes().isBlank())
                ? utilisateur.getNiveauEtudes() : "L3";
        List<String> competences = (utilisateur.getCompetences() != null)
                ? new ArrayList<>(utilisateur.getCompetences()) : new ArrayList<>();

        return EtudiantProfilDTO.builder()
                .id(utilisateur.getId())
                .filiere(filiere)
                .niveau(niveau)
                .competences(competences)
                .motsClesSujetSouhaite(filiere)
                .build();
    }

    /**
     * Construit la liste des DTOs d'offres à partir des entités OffreStage réelles.
     */
    private List<OffreEntrepriseDTO> construireOffresDepuisOffreStage(List<OffreStage> offres) {
        return offres.stream()
                .map(this::construireOffreDepuisOffreStage)
                .collect(Collectors.toList());
    }

    private OffreEntrepriseDTO construireOffreDepuisOffreStage(OffreStage offre) {
        String entrepriseNom = (offre.getEntreprise() != null && offre.getEntreprise().getNom() != null)
                ? offre.getEntreprise().getNom() : "Entreprise";
        String secteur = (offre.getDomaine() != null && !offre.getDomaine().isBlank())
                ? offre.getDomaine() : "Général";
        String sujet = ((offre.getTitre() != null ? offre.getTitre() : "") + " "
                + (offre.getDescription() != null ? offre.getDescription() : "")).trim();
        List<String> competences = (offre.getCompetences() != null)
                ? new ArrayList<>(offre.getCompetences()) : new ArrayList<>();

        return OffreEntrepriseDTO.builder()
                .id(offre.getId())
                .nomEntreprise(entrepriseNom)
                .secteurActivite(secteur)
                .sujetOffre(sujet)
                .competencesRequises(competences)
                .build();
    }

    /**
     * Fallback : Construit la liste des DTOs d'offres à partir des entreprises.
     */
    private List<OffreEntrepriseDTO> construireOffresEntreprise(List<Entreprise> entreprises) {
        return entreprises.stream()
                .map(this::construireOffreEntreprise)
                .collect(Collectors.toList());
    }

    private OffreEntrepriseDTO construireOffreEntreprise(Entreprise entreprise) {
        String sujetOffre = "Offre de stage chez " + (entreprise.getNom() != null ? entreprise.getNom() : "Entreprise");
        List<String> competencesRequises = new ArrayList<>();

        return OffreEntrepriseDTO.builder()
                .id(entreprise.getId())
                .nomEntreprise(entreprise.getNom() != null ? entreprise.getNom() : "")
                .secteurActivite(entreprise.getSecteurActivite() != null ? entreprise.getSecteurActivite() : "Général")
                .sujetOffre(sujetOffre)
                .competencesRequises(competencesRequises)
                .build();
    }

    public AdequationResponseDTO analyserAdequation(Long etudiantId, Long offreId) {
        log.info("Demande d'analyse d'adéquation pour l'étudiant {} et l'offre {}", etudiantId, offreId);

        Utilisateur utilisateur = userRepository.findById(etudiantId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec id : " + etudiantId));

        OffreStage offre = offreStageRepository.findById(offreId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée avec id : " + offreId));

        EtudiantProfilDTO profilEtudiant = construireProfilEtudiant(utilisateur);
        OffreEntrepriseDTO dtoOffre = construireOffreDepuisOffreStage(offre);

        try {
            String url = recommandationServiceUrl + adequationEndpoint;
            AdequationRequestDTO request = AdequationRequestDTO.builder()
                    .etudiant(profilEtudiant)
                    .offre(dtoOffre)
                    .build();

            AdequationResponseDTO reponse = restTemplate.postForObject(
                    url,
                    request,
                    AdequationResponseDTO.class
            );

            if (reponse != null) {
                return reponse;
            }
        } catch (Exception e) {
            log.warn("Service Python de recommandation indisponible ou en erreur ({}). Utilisation de l'analyse locale.", e.getMessage());
        }

        return calculerAdequationLocal(offreId, profilEtudiant, dtoOffre);
    }

    private AdequationResponseDTO calculerAdequationLocal(Long offreId, EtudiantProfilDTO etudiant, OffreEntrepriseDTO offre) {
        List<String> etudiantSkills = etudiant.getCompetences() != null ? etudiant.getCompetences() : Collections.emptyList();
        List<String> requiredSkills = offre.getCompetencesRequises() != null ? offre.getCompetencesRequises() : Collections.emptyList();

        List<String> acquises = new ArrayList<>();
        List<String> manquantes = new ArrayList<>();

        for (String req : requiredSkills) {
            String reqNorm = req.trim().toLowerCase();
            boolean match = etudiantSkills.stream().anyMatch(es -> {
                String esNorm = es.trim().toLowerCase();
                return esNorm.equals(reqNorm) || esNorm.contains(reqNorm) || reqNorm.contains(esNorm);
            });
            if (match) {
                acquises.add(req);
            } else {
                manquantes.add(req);
            }
        }

        int scorePct;
        if (!requiredSkills.isEmpty()) {
            scorePct = (int) Math.round(((double) acquises.size() / requiredSkills.size()) * 100);
        } else {
            scorePct = 75; // Bonne correspondance par défaut si aucune compétence requise spécifique
        }

        List<String> conseils = new ArrayList<>();
        if (scorePct >= 70) {
            conseils.add("Excellente adéquation ! Votre profil correspond étroitement aux attentes de l'entreprise.");
        } else if (scorePct >= 40) {
            conseils.add("Bonne correspondance globale. Mettez en valeur vos projets pratiques dans votre candidature.");
        } else {
            conseils.add("Cette offre propose des compétences complémentaires. Une belle opportunité pour monter en compétences !");
        }

        if (!manquantes.isEmpty()) {
            String missingSample = String.join(", ", manquantes.stream().limit(3).toList());
            conseils.add("Compétences à approfondir pour cette opportunité : " + missingSample + ".");
        }

        return AdequationResponseDTO.builder()
                .offreId(offreId)
                .score((double) scorePct / 100.0)
                .scorePourcentage(scorePct)
                .competencesAcquises(acquises)
                .competencesManquantes(manquantes)
                .conseils(conseils)
                .build();
    }

    /**
     * Exception applicative pour les ressources non trouvées.
     */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
