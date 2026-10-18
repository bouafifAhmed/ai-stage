package com.gestionstages.dto.recommandation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyse d'adéquation renvoyée par le microservice Python.
 *
 * source : "ia" (analyse rédigée par Claude), "regles" (règles côté Python)
 * ou "regles-spring" (service Python injoignable, calcul local).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdequationResponseDTO {
    private Long offreId;
    private Double score;
    private Integer scorePourcentage;
    @Builder.Default
    private List<String> competencesAcquises = new ArrayList<>();
    @Builder.Default
    private List<String> competencesManquantes = new ArrayList<>();
    @Builder.Default
    private List<String> conseils = new ArrayList<>();
    @Builder.Default
    private List<CorrespondanceCompetenceDTO> correspondances = new ArrayList<>();
    private String synthese;
    @Builder.Default
    private String source = "regles";
    private String modele;
}
