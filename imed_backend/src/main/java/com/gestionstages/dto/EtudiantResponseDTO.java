package com.gestionstages.dto;

import com.gestionstages.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.List;

public record EtudiantResponseDTO(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String filiere,
        String niveauEtudes,
        List<String> competences,
        boolean cvPresent,
        String cvNomFichier,
        LocalDateTime cvDateDepot,
        boolean actif,
        LocalDateTime dateCreation
) {
    public static EtudiantResponseDTO from(Utilisateur utilisateur) {
        return new EtudiantResponseDTO(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getFiliere(),
                utilisateur.getNiveauEtudes(),
                utilisateur.getCompetences(),
                utilisateur.getCvNomFichier() != null,
                utilisateur.getCvNomFichier(),
                utilisateur.getCvDateDepot(),
                utilisateur.isActif(),
                utilisateur.getDateCreation()
        );
    }
}
