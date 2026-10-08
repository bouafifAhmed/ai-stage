package com.gestionstages.dto.recommandation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO du profil d'un étudiant envoyé au microservice Python.
 * Utilisé pour construire le vecteur textuel côté Python (TF-IDF).
 *
 * Champs :
 * - id : Identifiant unique de l'étudiant
 * - filiere : Filière d'études (ex: "Informatique", "Génie Civil")
 * - niveau : Niveau d'études (ex: "L1", "L2", "L3", "M1", "M2")
 * - competences : Liste des compétences acquises (ex: ["Python", "Java", "SQL"])
 * - motsClesSujetSouhaite : Mots-clés décrivant le sujet/domaine souhaité (optionnel)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EtudiantProfilDTO {

    private Long id;

    private String filiere;

    private String niveau;

    private List<String> competences;

    private String motsClesSujetSouhaite;
}
