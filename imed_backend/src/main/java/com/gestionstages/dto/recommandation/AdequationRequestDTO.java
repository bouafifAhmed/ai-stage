package com.gestionstages.dto.recommandation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdequationRequestDTO {
    private EtudiantProfilDTO etudiant;
    private OffreEntrepriseDTO offre;
}
