package com.gestionstages.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offres_stage")
public class OffreStage {
    public enum ModeTravail { PRESENTIEL, HYBRIDE, DISTANCIEL }
    public enum StatutOffre { BROUILLON, PUBLIEE, CLOTUREE, ARCHIVEE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 180) private String titre;
    @Lob @Column(nullable = false) private String description;
    @Column(nullable = false, length = 120) private String domaine;
    @Column(nullable = false, length = 180) private String localisation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ModeTravail mode;
    @Column(name = "type_stage", nullable = false, length = 80) private String typeStage;
    @Column(name = "duree_mois") private Integer dureeMois;
    @Column(name = "date_debut") private LocalDate dateDebut;
    @Column(name = "date_fin") private LocalDate dateFin;
    @Column(name = "date_limite", nullable = false) private LocalDate dateLimite;
    @ElementCollection
    @CollectionTable(name = "offre_competences", joinColumns = @JoinColumn(name = "offre_id"))
    @Column(name = "competence", length = 100)
    private List<String> competences = new ArrayList<>();
    @Column(name = "niveau_requis", length = 100) private String niveauRequis;
    @Column(name = "nombre_places", nullable = false) private Integer nombrePlaces;
    @Column(precision = 12, scale = 2) private BigDecimal remuneration;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutOffre statut;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false) private Entreprise entreprise;
    @Column(name = "date_creation", nullable = false, updatable = false) private LocalDateTime dateCreation;
    @Column(name = "date_modification", nullable = false) private LocalDateTime dateModification;

    @PrePersist void prePersist() {
        if (statut == null) statut = StatutOffre.BROUILLON;
        if (dateCreation == null) dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }
    @PreUpdate void preUpdate() { dateModification = LocalDateTime.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTitre() { return titre; } public void setTitre(String v) { titre = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getDomaine() { return domaine; } public void setDomaine(String v) { domaine = v; }
    public String getLocalisation() { return localisation; } public void setLocalisation(String v) { localisation = v; }
    public ModeTravail getMode() { return mode; } public void setMode(ModeTravail v) { mode = v; }
    public String getTypeStage() { return typeStage; } public void setTypeStage(String v) { typeStage = v; }
    public Integer getDureeMois() { return dureeMois; } public void setDureeMois(Integer v) { dureeMois = v; }
    public LocalDate getDateDebut() { return dateDebut; } public void setDateDebut(LocalDate v) { dateDebut = v; }
    public LocalDate getDateFin() { return dateFin; } public void setDateFin(LocalDate v) { dateFin = v; }
    public LocalDate getDateLimite() { return dateLimite; } public void setDateLimite(LocalDate v) { dateLimite = v; }
    public List<String> getCompetences() { return competences; }
    public void setCompetences(List<String> v) { competences = v == null ? new ArrayList<>() : new ArrayList<>(v); }
    public String getNiveauRequis() { return niveauRequis; } public void setNiveauRequis(String v) { niveauRequis = v; }
    public Integer getNombrePlaces() { return nombrePlaces; } public void setNombrePlaces(Integer v) { nombrePlaces = v; }
    public BigDecimal getRemuneration() { return remuneration; } public void setRemuneration(BigDecimal v) { remuneration = v; }
    public StatutOffre getStatut() { return statut; } public void setStatut(StatutOffre v) { statut = v; }
    public Entreprise getEntreprise() { return entreprise; } public void setEntreprise(Entreprise v) { entreprise = v; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateModification() { return dateModification; }
}
