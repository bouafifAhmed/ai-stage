package com.gestionstages.dto;

import com.gestionstages.model.Entreprise;
import com.gestionstages.model.StatutValidation;
import com.gestionstages.model.TailleEntreprise;

import java.time.LocalDateTime;

public record EntrepriseResponseDTO(
        Long id,
        String nom,
        String adresse,
        String ville,
        String secteurActivite,
        TailleEntreprise taille,
        String emailContact,
        String telephone,
        String siteWeb,
        StatutValidation statutValidation,
        LocalDateTime dateInscription,
        String motifRejet
) {
    public static EntrepriseResponseDTO from(Entreprise entreprise) {
        return new EntrepriseResponseDTO(
                entreprise.getId(),
                entreprise.getNom(),
                entreprise.getAdresse(),
                entreprise.getVille(),
                entreprise.getSecteurActivite(),
                entreprise.getTaille(),
                entreprise.getEmailContact(),
                entreprise.getTelephone(),
                entreprise.getSiteWeb(),
                entreprise.getStatutValidation(),
                entreprise.getDateInscription(),
                entreprise.getMotifRejet()
        );
    }
}
