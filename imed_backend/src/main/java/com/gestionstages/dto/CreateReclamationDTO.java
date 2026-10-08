package com.gestionstages.dto;

import com.gestionstages.model.TypeReclamation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateReclamationDTO {

    @NotNull(message = "Le type de réclamation est obligatoire")
    private TypeReclamation typeReclamation;

    @NotBlank(message = "L'objet est obligatoire")
    @Size(max = 150, message = "L'objet ne peut pas dépasser 150 caractères")
    private String objet;

    @NotBlank(message = "Le message initial est obligatoire")
    private String messageInitial;

    // Getters et Setters
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

    public String getMessageInitial() {
        return messageInitial;
    }

    public void setMessageInitial(String messageInitial) {
        this.messageInitial = messageInitial;
    }
}
