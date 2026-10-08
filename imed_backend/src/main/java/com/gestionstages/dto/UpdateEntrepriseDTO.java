package com.gestionstages.dto;

import com.gestionstages.model.TailleEntreprise;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateEntrepriseDTO(
        @Size(min = 1, max = 150, message = "Le nom doit contenir entre 1 et 150 caractères")
        String nom,

        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
        String adresse,

        @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
        String ville,

        @Size(max = 150, message = "Le secteur d'activité ne doit pas dépasser 150 caractères")
        String secteurActivite,

        TailleEntreprise taille,

        @Email(message = "L'email de contact n'est pas valide")
        @Size(min = 1, max = 190, message = "L'email doit contenir entre 1 et 190 caractères")
        String emailContact,

        @Size(max = 30, message = "Le téléphone ne doit pas dépasser 30 caractères")
        String telephone,

        @Size(max = 255, message = "Le site web ne doit pas dépasser 255 caractères")
        String siteWeb
) {
}
