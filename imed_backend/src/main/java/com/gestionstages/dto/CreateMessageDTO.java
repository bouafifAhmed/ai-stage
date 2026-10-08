package com.gestionstages.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateMessageDTO {

    @NotBlank(message = "Le contenu du message est obligatoire")
    private String contenu;

    private String pieceJointe;

    // Getters et Setters
    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getPieceJointe() {
        return pieceJointe;
    }

    public void setPieceJointe(String pieceJointe) {
        this.pieceJointe = pieceJointe;
    }
}
