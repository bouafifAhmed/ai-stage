package com.gestionstages.dto.recommandation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Verdict pour une compétence requise par l'offre : acquise ou non, et pourquoi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrespondanceCompetenceDTO {
    private String competenceRequise;
    private boolean acquise;
    private String justification;
}
