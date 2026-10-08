package com.gestionstages.dto;

import java.time.LocalDateTime;

public class MessageResponseDTO {

    private Long id;
    private String contenu;
    private String pieceJointe;
    private LocalDateTime dateEnvoi;
    private String nomAuteur;
    private String roleAuteur;

    // Constructeurs
    public MessageResponseDTO() {
    }

    public MessageResponseDTO(Long id, String contenu, String pieceJointe,
                               LocalDateTime dateEnvoi, String nomAuteur, String roleAuteur) {
        this.id = id;
        this.contenu = contenu;
        this.pieceJointe = pieceJointe;
        this.dateEnvoi = dateEnvoi;
        this.nomAuteur = nomAuteur;
        this.roleAuteur = roleAuteur;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public String getNomAuteur() {
        return nomAuteur;
    }

    public void setNomAuteur(String nomAuteur) {
        this.nomAuteur = nomAuteur;
    }

    public String getRoleAuteur() {
        return roleAuteur;
    }

    public void setRoleAuteur(String roleAuteur) {
        this.roleAuteur = roleAuteur;
    }
}
