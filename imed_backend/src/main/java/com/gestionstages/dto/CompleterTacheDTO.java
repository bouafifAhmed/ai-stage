package com.gestionstages.dto;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

public record CompleterTacheDTO(
        @Size(max = 20000, message = "Le commentaire ne doit pas dépasser 20000 caractères")
        String commentaireEtudiant,

        @Null(message = "Le chemin de pièce jointe est géré uniquement par le serveur")
        String pieceJointeEtudiant
) {
}
