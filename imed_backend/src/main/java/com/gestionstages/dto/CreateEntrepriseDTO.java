package com.gestionstages.dto;

import com.gestionstages.model.TailleEntreprise;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEntrepriseDTO(
        @NotBlank(message = "Le nom de l'entreprise est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nom,

        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
        String adresse,

        @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
        String ville,

        @Size(max = 150, message = "Le secteur d'activité ne doit pas dépasser 150 caractères")
        String secteurActivite,

        TailleEntreprise taille,

        @NotBlank(message = "L'email de contact est obligatoire")
        @Email(message = "L'email de contact n'est pas valide")
        @Size(max = 190, message = "L'email ne doit pas dépasser 190 caractères")
        String emailContact,

        @Size(max = 30, message = "Le téléphone ne doit pas dépasser 30 caractères")
        String telephone,

        @Size(max = 255, message = "Le site web ne doit pas dépasser 255 caractères")
        String siteWeb,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères")
        String motDePasse
) {
}
