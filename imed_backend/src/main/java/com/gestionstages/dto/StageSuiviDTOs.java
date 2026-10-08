package com.gestionstages.dto;

import com.gestionstages.model.Candidature;
import com.gestionstages.model.StatutStage;
import com.gestionstages.model.StatutTacheAssignee;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class StageSuiviDTOs {
    private StageSuiviDTOs() {}

    public record SignerStageRequest(
            @jakarta.validation.constraints.NotBlank String signatureBase64
    ) {}

    public record EcheanceCalendrierDTO(
            Long tacheId,
            Long stageId,
            String titre,
            LocalDate dateEcheance,
            StatutTacheAssignee statut,
            String nomEtudiant,
            String entrepriseNom,
            String offreTitre
    ) {}

    public record StageClotureResponseDTO(
            Long stageId,
            StatutStage statutStage,
            boolean signatureEtudiantPresente,
            boolean signatureEncadrantPresente,
            LocalDateTime dateSignatureEtudiant,
            LocalDateTime dateSignatureEncadrant,
            LocalDateTime dateCloture,
            String nomEtudiant,
            String nomEncadrant,
            String entrepriseNom,
            String offreTitre,
            boolean peutSignerEtudiant,
            boolean peutSignerEncadrant
    ) {
        public static StageClotureResponseDTO from(Candidature stage, UtilisateurContext ctx) {
            return new StageClotureResponseDTO(
                    stage.getId(),
                    stage.getStatutStage(),
                    stage.getSignatureEtudiantPath() != null,
                    stage.getSignatureEncadrantPath() != null,
                    stage.getDateSignatureEtudiant(),
                    stage.getDateSignatureEncadrant(),
                    stage.getDateCloture(),
                    nomComplet(stage.getEtudiant().getPrenom(), stage.getEtudiant().getNom()),
                    stage.getEncadrant() == null
                            ? null
                            : nomComplet(stage.getEncadrant().getPrenom(), stage.getEncadrant().getNom()),
                    stage.getOffre().getEntreprise().getNom(),
                    stage.getOffre().getTitre(),
                    ctx.peutSignerEtudiant(),
                    ctx.peutSignerEncadrant()
            );
        }

        private static String nomComplet(String prenom, String nom) {
            return (prenom + " " + nom).trim();
        }
    }

    public record UtilisateurContext(
            boolean peutSignerEtudiant,
            boolean peutSignerEncadrant
    ) {}
}
