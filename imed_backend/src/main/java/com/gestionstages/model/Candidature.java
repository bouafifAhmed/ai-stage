package com.gestionstages.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidatures", uniqueConstraints = @UniqueConstraint(
        name = "uk_candidature_offre_etudiant", columnNames = {"offre_id", "etudiant_id"}))
public class Candidature {
    public enum StatutCandidature { EN_ATTENTE, ACCEPTEE, REFUSEE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false) private OffreStage offre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false) private Utilisateur etudiant;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encadrant_id") private Utilisateur encadrant;
    @Lob private String message;
    @Column(name = "date_candidature", nullable = false, updatable = false) private LocalDateTime dateCandidature;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutCandidature statut;
    @Enumerated(EnumType.STRING) @Column(name = "statut_stage", nullable = false, length = 20)
    private StatutStage statutStage;
    @Column(name = "signature_etudiant_path", length = 500) private String signatureEtudiantPath;
    @Column(name = "signature_encadrant_path", length = 500) private String signatureEncadrantPath;
    @Column(name = "date_signature_etudiant") private LocalDateTime dateSignatureEtudiant;
    @Column(name = "date_signature_encadrant") private LocalDateTime dateSignatureEncadrant;
    @Column(name = "date_cloture") private LocalDateTime dateCloture;

    @PrePersist void prePersist() {
        if (dateCandidature == null) dateCandidature = LocalDateTime.now();
        if (statut == null) statut = StatutCandidature.EN_ATTENTE;
        if (statutStage == null) statutStage = StatutStage.ACTIF;
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public OffreStage getOffre() { return offre; } public void setOffre(OffreStage v) { offre = v; }
    public Utilisateur getEtudiant() { return etudiant; } public void setEtudiant(Utilisateur v) { etudiant = v; }
    public Utilisateur getEncadrant() { return encadrant; }
    public void setEncadrant(Utilisateur v) { encadrant = v; }
    public String getMessage() { return message; } public void setMessage(String v) { message = v; }
    public LocalDateTime getDateCandidature() { return dateCandidature; }
    public void setDateCandidature(LocalDateTime v) { dateCandidature = v; }
    public StatutCandidature getStatut() { return statut; } public void setStatut(StatutCandidature v) { statut = v; }
    public StatutStage getStatutStage() { return statutStage; }
    public void setStatutStage(StatutStage v) { statutStage = v; }
    public String getSignatureEtudiantPath() { return signatureEtudiantPath; }
    public void setSignatureEtudiantPath(String v) { signatureEtudiantPath = v; }
    public String getSignatureEncadrantPath() { return signatureEncadrantPath; }
    public void setSignatureEncadrantPath(String v) { signatureEncadrantPath = v; }
    public LocalDateTime getDateSignatureEtudiant() { return dateSignatureEtudiant; }
    public void setDateSignatureEtudiant(LocalDateTime v) { dateSignatureEtudiant = v; }
    public LocalDateTime getDateSignatureEncadrant() { return dateSignatureEncadrant; }
    public void setDateSignatureEncadrant(LocalDateTime v) { dateSignatureEncadrant = v; }
    public LocalDateTime getDateCloture() { return dateCloture; }
    public void setDateCloture(LocalDateTime v) { dateCloture = v; }
}
