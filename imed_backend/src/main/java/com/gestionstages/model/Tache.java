package com.gestionstages.model;

import com.gestionstages.model.Candidature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "taches")
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private Candidature stage;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 150)
    private String titre;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "piece_jointe", length = 500)
    private String pieceJointe;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_approbation", nullable = false, length = 20)
    private StatutApprobation statutApprobation = StatutApprobation.EN_ATTENTE;

    @Lob
    @Column(name = "commentaire_encadrant", columnDefinition = "TEXT")
    private String commentaireEncadrant;

    @Column(name = "date_saisie", nullable = false, updatable = false)
    private LocalDateTime dateSaisie;

    @Column(name = "date_approbation")
    private LocalDateTime dateApprobation;

    @PrePersist
    void prePersist() {
        if (dateSaisie == null) {
            dateSaisie = LocalDateTime.now();
        }
        if (statutApprobation == null) {
            statutApprobation = StatutApprobation.EN_ATTENTE;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Candidature getStage() {
        return stage;
    }

    public void setStage(Candidature stage) {
        this.stage = stage;
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
