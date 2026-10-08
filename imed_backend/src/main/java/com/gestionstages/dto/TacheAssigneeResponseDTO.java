package com.gestionstages.dto;

import com.gestionstages.model.StatutTacheAssignee;
import com.gestionstages.model.TacheAssignee;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TacheAssigneeResponseDTO(
        Long id,
        Long stageId,
        Long encadrantId,
        String nomEncadrant,
        Long etudiantId,
        String nomEtudiant,
        String titre,
        String description,
        LocalDate dateEcheance,
        StatutTacheAssignee statut,
        LocalDateTime dateAssignation,
        String commentaireEtudiant,
        String pieceJointeEtudiant,
        String commentaireEncadrant,
        LocalDateTime dateCompletion,
        LocalDateTime dateValidation
) {
    public static TacheAssigneeResponseDTO from(TacheAssignee tache, String nomPieceJointe) {
        return new TacheAssigneeResponseDTO(
                tache.getId(),
                tache.getStage().getId(),
                tache.getEncadrant().getId(),
                nomComplet(tache.getEncadrant().getPrenom(), tache.getEncadrant().getNom()),
                tache.getStage().getEtudiant().getId(),
                nomComplet(tache.getStage().getEtudiant().getPrenom(), tache.getStage().getEtudiant().getNom()),
                tache.getTitre(),
                tache.getDescription(),
                tache.getDateEcheance(),
                tache.getStatut(),
                tache.getDateAssignation(),
                tache.getCommentaireEtudiant(),
                nomPieceJointe,
                tache.getCommentaireEncadrant(),
                tache.getDateCompletion(),
                tache.getDateValidation()
        );
    }

    private static String nomComplet(String prenom, String nom) {
        return (prenom + " " + nom).trim();
    }
}
