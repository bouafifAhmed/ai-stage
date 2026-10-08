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
@Slf4j
@Service
public class RecommandationClientService {

    @Value("${recommandation.service.url}")
    private String recommandationServiceUrl;

    @Value("${recommandation.service.endpoint.recommander}")
    private String recommanderEndpoint;

    @Value("${recommandation.service.endpoint.health}")
    private String healthEndpoint;

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final CandidatureRepository candidatureRepository;

    public RecommandationClientService(
            RestTemplate restTemplate,
            UserRepository userRepository,
            EntrepriseRepository entrepriseRepository,
            CandidatureRepository candidatureRepository
    ) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.candidatureRepository = candidatureRepository;
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

            // Étape 2 : Récupérer toutes les entreprises
            List<Entreprise> entreprises = entrepriseRepository.findAll();

            if (entreprises.isEmpty()) {
                log.warn("Aucune entreprise disponible pour les recommandations");
                return Collections.emptyList();
            }

            // Étape 3 : Transformer en DTOs

            // Construire le profil étudiant
            EtudiantProfilDTO profilEtudiant = construireProfilEtudiant(utilisateur);

            // Construire la liste des offres entreprise
            List<OffreEntrepriseDTO> offresEntreprise = construireOffresEntreprise(entreprises);

            if (offresEntreprise.isEmpty()) {
                log.warn("Aucune offre disponible pour les recommandations");
                return Collections.emptyList();
            }

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
     *
     * @param utilisateur Entité JPA Utilisateur
     * @return EtudiantProfilDTO
     */
    private EtudiantProfilDTO construireProfilEtudiant(Utilisateur utilisateur) {
        // Créer un profil minimal avec les informations disponibles
        return EtudiantProfilDTO.builder()
                .id(utilisateur.getId())
                .filiere("General") // Valeur par défaut si le champ n'existe pas
                .niveau("L3") // Valeur par défaut
                .competences(new ArrayList<>()) // Liste vide
                .motsClesSujetSouhaite(null) // Optionnel
                .build();
    }

    /**
     * Construit la liste des DTOs d'offres entreprise à partir des entités JPA.
     *
     * @param entreprises Toutes les entreprises de la base de données
     * @return List<OffreEntrepriseDTO>
     */
    private List<OffreEntrepriseDTO> construireOffresEntreprise(List<Entreprise> entreprises) {
        return entreprises.stream()
                .map(this::construireOffreEntreprise)
                .collect(Collectors.toList());
    }

    /**
     * Construit un DTO d'offre entreprise.
     *
     * @param entreprise Entité JPA Entreprise
     * @return OffreEntrepriseDTO
     */
    private OffreEntrepriseDTO construireOffreEntreprise(Entreprise entreprise) {
        // Construire une description d'offre simple
        String sujetOffre = "Offre de stage chez " + (entreprise.getNom() != null ? entreprise.getNom() : "Entreprise");

        // Compétences requises (optionnel)
        List<String> competencesRequises = new ArrayList<>();

        return OffreEntrepriseDTO.builder()
                .id(entreprise.getId())
                .nomEntreprise(entreprise.getNom() != null ? entreprise.getNom() : "")
                .secteurActivite(entreprise.getSecteurActivite() != null ? entreprise.getSecteurActivite() : "Général")
                .sujetOffre(sujetOffre)
                .competencesRequises(competencesRequises)
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
