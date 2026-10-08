package com.gestionstages.dto;

import com.gestionstages.model.Candidature;
import com.gestionstages.model.NotificationEtudiant;
import com.gestionstages.model.TypeNotification;
import com.gestionstages.model.OffreStage;
import com.gestionstages.model.Utilisateur;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class StageDTOs {
    private StageDTOs() {}

    public record OffreRequest(
            @NotBlank @Size(max = 180) String titre,
            @NotBlank String description,
            @NotBlank @Size(max = 120) String domaine,
            @NotBlank @Size(max = 180) String localisation,
            @NotNull OffreStage.ModeTravail mode,
            @NotBlank @Size(max = 80) String typeStage,
            @Positive Integer dureeMois,
            LocalDate dateDebut,
            LocalDate dateFin,
            @NotNull LocalDate dateLimite,
            List<@NotBlank @Size(max = 100) String> competences,
            @Size(max = 100) String niveauRequis,
            @NotNull @Positive Integer nombrePlaces,
            @PositiveOrZero BigDecimal remuneration,
            @NotNull OffreStage.StatutOffre statut
    ) {}

    public record OffreResponse(
            Long id, String titre, String description, String domaine, String localisation,
            OffreStage.ModeTravail mode, String typeStage, Integer dureeMois,
            LocalDate dateDebut, LocalDate dateFin, LocalDate dateLimite,
            List<String> competences, String niveauRequis, Integer nombrePlaces,
            BigDecimal remuneration, OffreStage.StatutOffre statut,
            Long entrepriseId, String entrepriseNom,
            LocalDateTime dateCreation, LocalDateTime dateModification,
            long placesOccupees, long placesRestantes
    ) {
        public static OffreResponse from(OffreStage o, long placesOccupees) {
            long placesRestantes = Math.max(0, o.getNombrePlaces() - placesOccupees);
            return new OffreResponse(o.getId(), o.getTitre(), o.getDescription(), o.getDomaine(),
                    o.getLocalisation(), o.getMode(), o.getTypeStage(), o.getDureeMois(),
                    o.getDateDebut(), o.getDateFin(), o.getDateLimite(), List.copyOf(o.getCompetences()),
                    o.getNiveauRequis(), o.getNombrePlaces(), o.getRemuneration(), o.getStatut(),
                    o.getEntreprise().getId(), o.getEntreprise().getNom(),
                    o.getDateCreation(), o.getDateModification(), placesOccupees, placesRestantes);
        }
    }

    public record ProfilRequest(
            @Size(max = 30) String telephone,
            @Size(max = 150) String filiere,
            @Size(max = 100) String niveauEtudes,
            List<@NotBlank @Size(max = 100) String> competences
    ) {}

    public record ProfilResponse(
            Long id, String nom, String prenom, String email, String telephone,
            String filiere, String niveauEtudes, List<String> competences,
            boolean cvPresent, String cvNomFichier, LocalDateTime cvDateDepot
    ) {
        public static ProfilResponse from(Utilisateur u) {
            return new ProfilResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(),
                    u.getTelephone(), u.getFiliere(), u.getNiveauEtudes(), List.copyOf(u.getCompetences()),
                    u.getCvNomFichier() != null, u.getCvNomFichier(), u.getCvDateDepot());
        }
    }

    public record CvExtractionResponse(
            String telephone,
            String filiere,
            String niveauEtudes,
            List<String> competences,
            boolean texteDetecte
    ) {
        public static CvExtractionResponse empty() {
            return new CvExtractionResponse(null, null, null, List.of(), false);
        }
    }

    public record CvUploadResponse(
            ProfilResponse profil,
            CvExtractionResponse extraction
    ) {}

    public record CandidatureRequest(
            @NotNull @Positive Long offreId,
            @NotBlank @Size(max = 5000) String message
    ) {}

    public record CandidatureStatutRequest(
            @NotNull Candidature.StatutCandidature statut
    ) {}

    public record CandidatureResponse(
            Long id, Long offreId, String offreTitre, String entrepriseNom,
            Long etudiantId, String etudiantNom,
            String etudiantPrenom, String etudiantEmail, String telephone, String filiere,
            String niveauEtudes, List<String> competences, String message,
            LocalDateTime dateCandidature, Candidature.StatutCandidature statut
    ) {
        public static CandidatureResponse from(Candidature c) {
            Utilisateur e = c.getEtudiant();
            return new CandidatureResponse(c.getId(), c.getOffre().getId(), c.getOffre().getTitre(),
                    c.getOffre().getEntreprise().getNom(), e.getId(), e.getNom(), e.getPrenom(),
                    e.getEmail(), e.getTelephone(), e.getFiliere(),
                    e.getNiveauEtudes(), List.copyOf(e.getCompetences()), c.getMessage(),
                    c.getDateCandidature(), c.getStatut());
        }
    }

    public record NotificationResponse(
            Long id,
            Long candidatureId,
            Long offreId,
            String offreTitre,
            String entrepriseNom,
            String message,
            boolean lue,
            LocalDateTime dateCreation,
            TypeNotification type,
            Long tacheId
    ) {
        public static NotificationResponse from(NotificationEtudiant notification) {
            Candidature candidature = notification.getCandidature();
            return new NotificationResponse(
                    notification.getId(),
                    candidature.getId(),
                    candidature.getOffre().getId(),
                    candidature.getOffre().getTitre(),
                    candidature.getOffre().getEntreprise().getNom(),
                    notification.getMessage(),
                    notification.isLue(),
                    notification.getDateCreation(),
                    notification.getType(),
                    notification.getTacheAssignee() == null
                            ? null
                            : notification.getTacheAssignee().getId()
            );
        }
    }

    public record NotificationCountResponse(long nonLues) {}
}
