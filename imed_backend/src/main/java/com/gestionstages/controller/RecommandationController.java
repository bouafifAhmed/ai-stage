package com.gestionstages.controller;

import com.gestionstages.dto.recommandation.RecommandationItemDTO;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.UserRepository;
import com.gestionstages.security.JwtTokenProvider;
import com.gestionstages.service.RecommandationClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Controller pour l'API de recommandations d'offres de stage.
 *
 * Endpoints :
 * - GET /api/recommandations : Obtenir les offres recommandées pour l'étudiant connecté
 *
 * Authentification :
 * - Tous les endpoints requièrent un JWT valide dans le header Authorization
 * - Rôle requis : ETUDIANT
 *
 * Architecture :
 * - Le controller récupère l'email de l'utilisateur connecté du JWT
 * - Récupère l'utilisateur depuis la BD
 * - Délègue au RecommandationClientService qui appelle le microservice Python
 * - Retourne les recommandations au frontend
 */
@Slf4j
@RestController
@RequestMapping("/api/recommandations")
public class RecommandationController {

    private final RecommandationClientService recommandationClientService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public RecommandationController(
            RecommandationClientService recommandationClientService,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository
    ) {
        this.recommandationClientService = recommandationClientService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    /**
     * Obtient les offres recommandées pour l'étudiant connecté.
     *
     * Processus :
     * 1. Récupérer l'email de l'utilisateur depuis le JWT
     * 2. Récupérer l'utilisateur depuis la BD
     * 3. Appeler le service de recommandation (qui contacte le microservice Python)
     * 4. Retourner la liste des offres triées par pertinence
     *
     * Réponse (200 OK) :
     * [
     *   {
     *     "offreId": 3,
     *     "nomEntreprise": "TechCorp",
     *     "score": 0.87
     *   },
     *   {
     *     "offreId": 1,
     *     "nomEntreprise": "DataLabs",
     *     "score": 0.65
     *   },
     *   ...
     * ]
     *
     * Erreurs possibles :
     * - 401 Unauthorized : JWT manquant ou invalide
     * - 403 Forbidden : Utilisateur n'a pas le rôle ETUDIANT
     * - 404 Not Found : Étudiant non trouvé dans la base de données
     * - 500 Internal Server Error : Erreur inattendue
     * - 200 OK + liste vide : Service Python indisponible (dégradation gracieuse)
     *
     * @param authHeader Header Authorization contenant le JWT (ex: "Bearer <token>")
     * @return ResponseEntity<List<RecommandationItemDTO>>
     */
    @GetMapping
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<List<RecommandationItemDTO>> obtenirRecommandations(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            log.info("Requête de recommandations reçue");

            // Extraire l'email (username) du JWT
            String token = authHeader.replace("Bearer ", "");
            String email = jwtTokenProvider.extractUsername(token);

            if (email == null || email.isEmpty()) {
                log.error("Impossible d'extraire l'email du JWT");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.info("Recherche de l'utilisateur avec email : {}", email);

            // Récupérer l'utilisateur par email
            Optional<Utilisateur> optUser = userRepository.findByEmail(email);
            if (optUser.isEmpty()) {
                log.error("Utilisateur non trouvé avec email : {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Utilisateur utilisateur = optUser.get();
            log.info("Calcul des recommandations pour l'utilisateur {}", utilisateur.getId());

            // Appeler le service de recommandation
            List<RecommandationItemDTO> recommandations = recommandationClientService
                    .obtenirRecommandations(utilisateur.getId());

            log.info("Retour de {} recommandations", recommandations.size());

            return ResponseEntity.ok(recommandations);

        } catch (RecommandationClientService.ResourceNotFoundException e) {
            log.error("Ressource non trouvée : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des recommandations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check du service de recommandation Python.
     * Endpoint optionnel pour diagnostiquer les problèmes de connectivité.
     *
     * @return ResponseEntity<Boolean>
     */
    @GetMapping("/health")
    public ResponseEntity<Boolean> healthCheck() {
        log.debug("Health check du service de recommandation");
        boolean available = recommandationClientService.isServiceAvailable();
        return ResponseEntity.ok(available);
    }
}
