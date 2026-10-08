package com.gestionstages.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTacheAssigneeDTO(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères")
        String titre,

        @NotBlank(message = "La description est obligatoire")
        @Size(max = 20000, message = "La description ne doit pas dépasser 20000 caractères")
        String description,

        @FutureOrPresent(message = "La date d'échéance ne peut pas être passée")
        LocalDate dateEcheance
) {
}
