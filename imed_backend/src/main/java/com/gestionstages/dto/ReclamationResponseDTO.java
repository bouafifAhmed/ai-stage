package com.gestionstages.dto;

import com.gestionstages.model.StatutReclamation;
import com.gestionstages.model.TypeReclamation;
import java.time.LocalDateTime;

public class ReclamationResponseDTO {

    private Long id;
    private TypeReclamation typeReclamation;
    private String objet;
    private StatutReclamation statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateCloture;
    private String nomEtudiant;
    private String nomTraitePar;

    // Constructeurs
    public ReclamationResponseDTO() {
    }

    public ReclamationResponseDTO(Long id, TypeReclamation typeReclamation, String objet,
                                   StatutReclamation statut, LocalDateTime dateCreation,
                                   LocalDateTime dateCloture, String nomEtudiant, String nomTraitePar) {
        this.id = id;
        this.typeReclamation = typeReclamation;
        this.objet = objet;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateCloture = dateCloture;
        this.nomEtudiant = nomEtudiant;
        this.nomTraitePar = nomTraitePar;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TypeReclamation getTypeReclamation() {
        return typeReclamation;
    }

    public void setTypeReclamation(TypeReclamation typeReclamation) {
        this.typeReclamation = typeReclamation;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public StatutReclamation getStatut() {
        return statut;
    }

    public void setStatut(StatutReclamation statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public String getNomEtudiant() {
        return nomEtudiant;
    }

    public void setNomEtudiant(String nomEtudiant) {
        this.nomEtudiant = nomEtudiant;
    }

    public String getNomTraitePar() {
        return nomTraitePar;
    }

    public void setNomTraitePar(String nomTraitePar) {
        this.nomTraitePar = nomTraitePar;
    }
}
