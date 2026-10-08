package com.gestionstages.controller;

import com.gestionstages.dto.*;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.ReclamationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReclamationController {

    private static final Logger logger = LoggerFactory.getLogger(ReclamationController.class);

    private final ReclamationService reclamationService;

    public ReclamationController(ReclamationService reclamationService) {
        this.reclamationService = reclamationService;
    }

    @PostMapping("/stages/{stageId}/reclamations")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> creerReclamation(
            @PathVariable Long stageId,
            @Valid @RequestBody CreateReclamationDTO dto,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non authentifié"));
            }

            ReclamationResponseDTO response = reclamationService.creerReclamation(
                    stageId, dto, currentUser.getId()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityException e) {
            logger.warn("Accès refusé lors de la création de réclamation : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Erreur de validation lors de la création de réclamation : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la création de réclamation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la création de la réclamation"));
        }
    }

    @GetMapping("/stages/{stageId}/reclamations")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENTREPRISE', 'CHEF_DEPT_STAGE', 'CHEF_DEPT_PEDAGOGIQUE')")
    public ResponseEntity<?> listerReclamationsParStage(
            @PathVariable Long stageId,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non authentifié"));
            }

            List<ReclamationResponseDTO> reclamations = reclamationService.listerReclamationsParStage(stageId);
            return ResponseEntity.ok(reclamations);
        } catch (Exception e) {
            logger.error("Erreur lors du listage des réclamations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du chargement des réclamations"));
        }
    }

    /**
     * Endpoint réservé à l'entreprise : liste toutes les réclamations de ses stagiaires
     */
    @GetMapping("/entreprise/reclamations")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public ResponseEntity<?> listerReclamationsEntreprise(
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non authentifié"));
            }
            if (currentUser.getEntreprise() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Cet utilisateur n'est pas rattaché à une entreprise"));
            }

            List<ReclamationResponseDTO> reclamations = reclamationService
                    .listerReclamationsParEntreprise(currentUser.getEntreprise().getId());
            return ResponseEntity.ok(reclamations);
        } catch (Exception e) {
            logger.error("Erreur lors du listage des réclamations de l'entreprise", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du chargement des réclamations"));
        }
    }

    @GetMapping("/reclamations/{id}")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENTREPRISE', 'CHEF_DEPT_STAGE', 'CHEF_DEPT_PEDAGOGIQUE')")
    public ResponseEntity<?> getDetailReclamation(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non authentifié"));
            }

            ReclamationDetailDTO detail = reclamationService.getDetailReclamation(id);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            logger.warn("Réclamation introuvable : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors du chargement du détail de la réclamation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du chargement du détail"));
        }
    }

    @PostMapping("/reclamations/{id}/messages")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENTREPRISE', 'CHEF_DEPT_STAGE', 'CHEF_DEPT_PEDAGOGIQUE')")
    public ResponseEntity<?> ajouterMessage(
            @PathVariable Long id,
            @Valid @RequestBody CreateMessageDTO dto,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non authentifié"));
            }

            String role = currentUser.getRole() != null ? currentUser.getRole().name() : "";
            MessageResponseDTO response = reclamationService.ajouterMessage(
                    id, dto, currentUser.getId(), role
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityException e) {
            logger.warn("Accès refusé lors de l'ajout de message : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Erreur lors de l'ajout de message : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout de message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'ajout du message"));
        }
    }

    @PutMapping("/reclamations/{id}/resoudre")
    @PreAuthorize("hasAnyRole('ENTREPRISE', 'CHEF_DEPT_STAGE', 'CHEF_DEPT_PEDAGOGIQUE')")
    public ResponseEntity<?> resoudreReclamation(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non authentifié"));
            }

            ReclamationResponseDTO response = reclamationService.resoudreReclamation(
                    id, currentUser.getId()
            );

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.warn("Accès refusé lors de la résolution : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Erreur lors de la résolution : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la résolution de la réclamation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la résolution"));
        }
    }

    @PutMapping("/reclamations/{id}/cloturer")
    @PreAuthorize("hasAnyRole('ENTREPRISE', 'CHEF_DEPT_STAGE', 'CHEF_DEPT_PEDAGOGIQUE')")
    public ResponseEntity<?> cloturerReclamation(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non authentifié"));
            }

            ReclamationResponseDTO response = reclamationService.cloturerReclamation(
                    id, currentUser.getId()
            );

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.warn("Accès refusé lors de la clôture : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Erreur lors de la clôture : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la clôture de la réclamation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la clôture"));
        }
    }

    @PutMapping("/reclamations/{id}/rouvrir")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> rouvrirReclamation(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur non authentifié"));
            }

            ReclamationResponseDTO response = reclamationService.rouvrirReclamation(
                    id, currentUser.getId()
            );

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.warn("Accès refusé lors de la réouverture : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Erreur lors de la réouverture : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la réouverture de la réclamation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la réouverture"));
        }
    }
}
