package com.gestionstages.dto;

import jakarta.validation.constraints.Size;

public record ValiderTacheDTO(
        @Size(max = 20000, message = "Le commentaire ne doit pas dépasser 20000 caractères")
        String commentaireEncadrant
) {
}
