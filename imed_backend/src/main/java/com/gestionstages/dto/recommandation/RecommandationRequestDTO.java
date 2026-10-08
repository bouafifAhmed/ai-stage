package com.gestionstages.dto.recommandation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de requête envoyé au microservice Python de recommandation.
 *
 * Contient :
 * - etudiant : Profil complet de l'étudiant
 * - offres : Liste COMPLÈTE de toutes les offres disponibles
 *
 * Note : Le microservice Python ne stocke rien. Spring Boot reste la source unique de vérité.
 * À chaque appel, on envoie l'étudiant ET la liste complète des offres actuelles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommandationRequestDTO {

    private EtudiantProfilDTO etudiant;

    private List<OffreEntrepriseDTO> offres;
}
