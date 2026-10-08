package com.gestionstages.dto.recommandation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'une recommandation individuelle : une offre avec son score de pertinence.
 *
 * Champs :
 * - offreId : Identifiant de l'offre recommandée
 * - nomEntreprise : Nom de l'entreprise (pour affichage frontend)
 * - score : Score de similarité cosinus entre 0 et 1, arrondi à 2 décimales
 *           (0 = aucune similiarité, 1 = identique)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommandationItemDTO {

    private Long offreId;

    private String nomEntreprise;

    private Double score;
}
