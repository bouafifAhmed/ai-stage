package com.gestionstages.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateEtudiantDTO(
        @Size(min = 1, max = 100, message = "Le nom doit contenir entre 1 et 100 caractères")
        String nom,

        @Size(min = 1, max = 100, message = "Le prénom doit contenir entre 1 et 100 caractères")
        String prenom,

        @Email(message = "L'email n'est pas valide")
        @Size(min = 1, max = 190, message = "L'email doit contenir entre 1 et 190 caractères")
        String email,

        @Size(max = 30, message = "Le téléphone ne doit pas dépasser 30 caractères")
        String telephone,

        @Size(max = 150, message = "La filière ne doit pas dépasser 150 caractères")
        String filiere,

        @Size(max = 100, message = "Le niveau d'études ne doit pas dépasser 100 caractères")
        String niveauEtudes,

        List<String> competences
) {
}
