package com.gestionstages.dto.recommandation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
}
