package com.gestionstages.dto;

import com.gestionstages.model.StatutReclamation;
import com.gestionstages.model.TypeReclamation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReclamationDetailDTO {

    private Long id;
    private TypeReclamation typeReclamation;
    private String objet;
    private StatutReclamation statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateCloture;
    private String nomEtudiant;
    private String nomTraitePar;
    private List<MessageResponseDTO> messages = new ArrayList<>();

    // Constructeurs
    public ReclamationDetailDTO() {
    }

    public ReclamationDetailDTO(ReclamationResponseDTO reclamation, List<MessageResponseDTO> messages) {
        this.id = reclamation.getId();
        this.typeReclamation = reclamation.getTypeReclamation();
        this.objet = reclamation.getObjet();
        this.statut = reclamation.getStatut();
        this.dateCreation = reclamation.getDateCreation();
        this.dateCloture = reclamation.getDateCloture();
        this.nomEtudiant = reclamation.getNomEtudiant();
        this.nomTraitePar = reclamation.getNomTraitePar();
        this.messages = messages;
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

    public List<MessageResponseDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponseDTO> messages) {
        this.messages = messages;
    }
}
