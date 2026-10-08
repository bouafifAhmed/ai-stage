package com.gestionstages.dto.recommandation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de réponse du microservice Python.
 *
 * Contient :
 * - recommandations : Liste des offres recommandées triées par score de pertinence (décroissant)
 *
 * Exemple de réponse :
 * {
 *   "recommandations": [
 *     {"offreId": 3, "nomEntreprise": "TechCorp", "score": 0.87},
 *     {"offreId": 1, "nomEntreprise": "DataLabs", "score": 0.65},
 *     ...
 *   ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommandationResponseDTO {

    private List<RecommandationItemDTO> recommandations;
}
