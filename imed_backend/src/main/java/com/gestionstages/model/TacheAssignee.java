package com.gestionstages.model;

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
@Table(name = "taches_assignees")
public class TacheAssignee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private Candidature stage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encadrant_id", nullable = false)
    private Utilisateur encadrant;

    @Column(nullable = false, length = 150)
    private String titre;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTacheAssignee statut;

    @Column(name = "date_assignation", nullable = false, updatable = false)
    private LocalDateTime dateAssignation;

    @Lob
    @Column(name = "commentaire_etudiant", columnDefinition = "TEXT")
    private String commentaireEtudiant;

    @Column(name = "piece_jointe_etudiant", length = 500)
    private String pieceJointeEtudiant;

    @Lob
    @Column(name = "commentaire_encadrant", columnDefinition = "TEXT")
    private String commentaireEncadrant;

    @Column(name = "date_completion")
    private LocalDateTime dateCompletion;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @PrePersist
    void prePersist() {
        if (statut == null) {
            statut = StatutTacheAssignee.ASSIGNEE;
        }
        if (dateAssignation == null) {
            dateAssignation = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Candidature getStage() { return stage; }
    public void setStage(Candidature stage) { this.stage = stage; }
    public Utilisateur getEncadrant() { return encadrant; }
    public void setEncadrant(Utilisateur encadrant) { this.encadrant = encadrant; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
    public StatutTacheAssignee getStatut() { return statut; }
    public void setStatut(StatutTacheAssignee statut) { this.statut = statut; }
    public LocalDateTime getDateAssignation() { return dateAssignation; }
    public void setDateAssignation(LocalDateTime dateAssignation) { this.dateAssignation = dateAssignation; }
    public String getCommentaireEtudiant() { return commentaireEtudiant; }
    public void setCommentaireEtudiant(String commentaireEtudiant) { this.commentaireEtudiant = commentaireEtudiant; }
    public String getPieceJointeEtudiant() { return pieceJointeEtudiant; }
    public void setPieceJointeEtudiant(String pieceJointeEtudiant) { this.pieceJointeEtudiant = pieceJointeEtudiant; }
    public String getCommentaireEncadrant() { return commentaireEncadrant; }
    public void setCommentaireEncadrant(String commentaireEncadrant) { this.commentaireEncadrant = commentaireEncadrant; }
    public LocalDateTime getDateCompletion() { return dateCompletion; }
    public void setDateCompletion(LocalDateTime dateCompletion) { this.dateCompletion = dateCompletion; }
    public LocalDateTime getDateValidation() { return dateValidation; }
    public void setDateValidation(LocalDateTime dateValidation) { this.dateValidation = dateValidation; }
}
