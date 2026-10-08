package com.gestionstages.dto;

import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;

public record UserResponseDTO(
        Long id,
        String nom,
        String prenom,
        String email,
        Role role
) {
    public static UserResponseDTO from(Utilisateur utilisateur) {
        return new UserResponseDTO(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getRole()
        );
    }
}
