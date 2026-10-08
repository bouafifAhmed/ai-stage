package com.gestionstages.dto.recommandation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO d'une offre de stage d'une entreprise.
 * Envoyé au microservice Python pour calcul de similarité.
 *
 * Champs :
 * - id : Identifiant unique de l'offre
 * - nomEntreprise : Nom de l'entreprise (ex: "Google", "Orange")
 * - secteurActivite : Secteur d'activité (ex: "IT", "Finance", "Santé")
 * - sujetOffre : Description textuelle du sujet/poste de stage
 * - competencesRequises : Compétences requises pour ce stage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OffreEntrepriseDTO {

    private Long id;

    private String nomEntreprise;

    private String secteurActivite;

    private String sujetOffre;

    private List<String> competencesRequises;
}
