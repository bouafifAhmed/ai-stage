package com.gestionstages.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tentatives_qcm")
public class TentativeQcm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Utilisateur etudiant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qcm_id", nullable = false)
    private Qcm qcm;

    @Column(name = "score_pourcentage", nullable = false)
    private Integer scorePourcentage;

    @Column(nullable = false)
    private boolean reussi;

    @Column(name = "numero_tentative", nullable = false)
    private Integer numeroTentative;

    @Column(name = "date_soumission", nullable = false, updatable = false)
    private LocalDateTime dateSoumission;

    @PrePersist
    void prePersist() {
        if (dateSoumission == null) {
            dateSoumission = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Utilisateur getEtudiant() { return etudiant; }
    public void setEtudiant(Utilisateur etudiant) { this.etudiant = etudiant; }
    public Qcm getQcm() { return qcm; }
    public void setQcm(Qcm qcm) { this.qcm = qcm; }
    public Integer getScorePourcentage() { return scorePourcentage; }
    public void setScorePourcentage(Integer scorePourcentage) { this.scorePourcentage = scorePourcentage; }
    public boolean isReussi() { return reussi; }
    public void setReussi(boolean reussi) { this.reussi = reussi; }
    public Integer getNumeroTentative() { return numeroTentative; }
    public void setNumeroTentative(Integer numeroTentative) { this.numeroTentative = numeroTentative; }
    public LocalDateTime getDateSoumission() { return dateSoumission; }
}
