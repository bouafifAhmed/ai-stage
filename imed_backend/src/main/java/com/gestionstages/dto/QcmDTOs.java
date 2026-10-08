package com.gestionstages.dto;

import com.gestionstages.model.OptionReponse;
import com.gestionstages.model.Qcm;
import com.gestionstages.model.Question;
import com.gestionstages.model.TypeQuestion;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class QcmDTOs {
    private QcmDTOs() {}

    public record OptionExamenResponse(Long id, String libelle, Integer ordre) {
        public static OptionExamenResponse from(OptionReponse option) {
            return new OptionExamenResponse(option.getId(), option.getLibelle(), option.getOrdre());
        }
    }

    public record QuestionExamenResponse(
            Long id,
            String enonce,
            TypeQuestion type,
            Integer points,
            Integer ordre,
            List<OptionExamenResponse> options
    ) {
        public static QuestionExamenResponse from(Question question, List<OptionReponse> options) {
            return new QuestionExamenResponse(
                    question.getId(),
                    question.getEnonce(),
                    question.getType(),
                    question.getPoints(),
                    question.getOrdre(),
                    options.stream().map(OptionExamenResponse::from).toList()
            );
        }
    }

    public record QcmExamenResponse(
            Long id,
            String titre,
            String domaine,
            Integer dureeMinutes,
            Integer noteMinimalePassage,
            Integer nombreMaxTentatives,
            List<QuestionExamenResponse> questions
    ) {
        public static QcmExamenResponse from(Qcm qcm, List<QuestionExamenResponse> questions) {
            return new QcmExamenResponse(
                    qcm.getId(),
                    qcm.getTitre(),
                    qcm.getDomaine(),
                    qcm.getDureeMinutes(),
                    qcm.getNoteMinimalePassage(),
                    qcm.getNombreMaxTentatives(),
                    questions
            );
        }
    }

    public record QcmStatutResponse(
            Long qcmId,
            String titre,
            boolean admissible,
            boolean reussi,
            int tentativesUtilisees,
            int tentativesRestantes,
            Integer meilleurScore
    ) {}

    public record ReponseQuestionRequest(
            @NotNull @Positive Long questionId,
            @NotNull @Positive Long optionId
    ) {}

    public record SoumettreQcmRequest(
            @NotEmpty List<ReponseQuestionRequest> reponses
    ) {}

    public record SoumettreQcmResponse(
            int scorePourcentage,
            boolean reussi,
            int noteMinimalePassage,
            int tentativesUtilisees,
            int tentativesRestantes,
            boolean admissible,
            String message
    ) {}
}
