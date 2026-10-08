package com.gestionstages.dto;

import com.gestionstages.model.StatutApprobation;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TacheResponseDTO {
    private Long id;
    private Long stageId;
    private LocalDate date;
    private String titre;
    private String description;
    private String pieceJointe;
    private StatutApprobation statutApprobation;
    private String commentaireEncadrant;
    private LocalDateTime dateSaisie;
    private LocalDateTime dateApprobation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPieceJointe() {
        return pieceJointe;
    }

    public void setPieceJointe(String pieceJointe) {
        this.pieceJointe = pieceJointe;
    }

    public StatutApprobation getStatutApprobation() {
        return statutApprobation;
    }

    public void setStatutApprobation(StatutApprobation statutApprobation) {
        this.statutApprobation = statutApprobation;
    }

    public String getCommentaireEncadrant() {
        return commentaireEncadrant;
    }

    public void setCommentaireEncadrant(String commentaireEncadrant) {
        this.commentaireEncadrant = commentaireEncadrant;
    }

    public LocalDateTime getDateSaisie() {
        return dateSaisie;
    }

    public void setDateSaisie(LocalDateTime dateSaisie) {
        this.dateSaisie = dateSaisie;
    }

    public LocalDateTime getDateApprobation() {
        return dateApprobation;
    }

    public void setDateApprobation(LocalDateTime dateApprobation) {
        this.dateApprobation = dateApprobation;
    }
}
