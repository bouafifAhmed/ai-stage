package com.gestionstages.service;

import com.gestionstages.dto.QcmDTOs.QcmExamenResponse;
import com.gestionstages.dto.QcmDTOs.QcmStatutResponse;
import com.gestionstages.dto.QcmDTOs.QuestionExamenResponse;
import com.gestionstages.dto.QcmDTOs.ReponseQuestionRequest;
import com.gestionstages.dto.QcmDTOs.SoumettreQcmRequest;
import com.gestionstages.dto.QcmDTOs.SoumettreQcmResponse;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.OptionReponse;
import com.gestionstages.model.Qcm;
import com.gestionstages.model.Question;
import com.gestionstages.model.Role;
import com.gestionstages.model.TentativeQcm;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.OptionReponseRepository;
import com.gestionstages.repository.QcmRepository;
import com.gestionstages.repository.QuestionRepository;
import com.gestionstages.repository.TentativeQcmRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class QcmEtudiantService {

    private final QcmRepository qcmRepository;
    private final QuestionRepository questionRepository;
    private final OptionReponseRepository optionReponseRepository;
    private final TentativeQcmRepository tentativeQcmRepository;

    public QcmEtudiantService(
            QcmRepository qcmRepository,
            QuestionRepository questionRepository,
            OptionReponseRepository optionReponseRepository,
            TentativeQcmRepository tentativeQcmRepository
    ) {
        this.qcmRepository = qcmRepository;
        this.questionRepository = questionRepository;
        this.optionReponseRepository = optionReponseRepository;
        this.tentativeQcmRepository = tentativeQcmRepository;
    }

    @Transactional(readOnly = true)
    public QcmExamenResponse obtenirQcmAdmissibilite(Utilisateur utilisateur) {
        etudiant(utilisateur);
        Qcm qcm = qcmActif();
        verifierPeutTenter(utilisateur, qcm);
        return construireExamen(qcm);
    }

    @Transactional(readOnly = true)
    public QcmStatutResponse statutAdmissibilite(Utilisateur utilisateur) {
        etudiant(utilisateur);
        Qcm qcm = qcmActif();
        return construireStatut(utilisateur, qcm);
    }

    @Transactional
    public SoumettreQcmResponse soumettreAdmissibilite(
            Utilisateur utilisateur,
            SoumettreQcmRequest request
    ) {
        Utilisateur etudiant = etudiant(utilisateur);
        Qcm qcm = qcmActif();
        verifierPeutTenter(etudiant, qcm);

        List<Question> questions = questionRepository.findByQcmIdOrderByOrdreAsc(qcm.getId());
        if (questions.isEmpty()) {
            throw new StageBusinessException(HttpStatus.CONFLICT, "Ce QCM ne contient aucune question");
        }

        Map<Long, Long> reponses = validerReponses(request, questions);
        int score = calculerScore(questions, reponses);
        boolean reussi = score >= qcm.getNoteMinimalePassage();

        long tentativesExistantes = tentativeQcmRepository.countByEtudiantIdAndQcmId(
                etudiant.getId(),
                qcm.getId()
        );
        TentativeQcm tentative = new TentativeQcm();
        tentative.setEtudiant(etudiant);
        tentative.setQcm(qcm);
        tentative.setScorePourcentage(score);
        tentative.setReussi(reussi);
        tentative.setNumeroTentative((int) tentativesExistantes + 1);
        tentativeQcmRepository.save(tentative);

        QcmStatutResponse statut = construireStatut(etudiant, qcm);
        return new SoumettreQcmResponse(
                score,
                reussi,
                qcm.getNoteMinimalePassage(),
                statut.tentativesUtilisees(),
                statut.tentativesRestantes(),
                statut.admissible(),
                messageResultat(reussi, statut)
        );
    }

    @Transactional(readOnly = true)
    public void verifierAdmissibilite(Utilisateur utilisateur) {
        Utilisateur etudiant = etudiant(utilisateur);
        Qcm qcm = qcmActif();
        if (!tentativeQcmRepository.existsByEtudiantIdAndQcmIdAndReussiTrue(
                etudiant.getId(),
                qcm.getId()
        )) {
            throw new StageBusinessException(
                    HttpStatus.CONFLICT,
                    "Vous devez réussir le QCM d'admissibilité avant de postuler à une offre"
            );
        }
    }

    private QcmExamenResponse construireExamen(Qcm qcm) {
        List<QuestionExamenResponse> questions = questionRepository.findByQcmIdOrderByOrdreAsc(qcm.getId())
                .stream()
                .map(question -> QuestionExamenResponse.from(
                        question,
                        optionReponseRepository.findByQuestionIdOrderByOrdreAsc(question.getId())
                ))
                .toList();
        return QcmExamenResponse.from(qcm, questions);
    }

    private QcmStatutResponse construireStatut(Utilisateur etudiant, Qcm qcm) {
        List<TentativeQcm> tentatives = tentativeQcmRepository
                .findByEtudiantIdAndQcmIdOrderByDateSoumissionDesc(etudiant.getId(), qcm.getId());
        int tentativesUtilisees = tentatives.size();
        int tentativesRestantes = Math.max(0, qcm.getNombreMaxTentatives() - tentativesUtilisees);
        boolean reussi = tentatives.stream().anyMatch(TentativeQcm::isReussi);
        Integer meilleurScore = tentatives.stream()
                .map(TentativeQcm::getScorePourcentage)
                .max(Integer::compareTo)
                .orElse(null);

        return new QcmStatutResponse(
                qcm.getId(),
                qcm.getTitre(),
                reussi,
                reussi,
                tentativesUtilisees,
                tentativesRestantes,
                meilleurScore
        );
    }

    private Map<Long, Long> validerReponses(SoumettreQcmRequest request, List<Question> questions) {
        if (request.reponses().size() != questions.size()) {
            throw new StageBusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Vous devez répondre à toutes les questions du QCM"
            );
        }

        Set<Long> questionsVues = new HashSet<>();
        Map<Long, Long> reponses = new HashMap<>();
        for (ReponseQuestionRequest reponse : request.reponses()) {
            if (!questionsVues.add(reponse.questionId())) {
                throw new StageBusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Chaque question ne doit être répondue qu'une seule fois"
                );
            }
            Question question = questions.stream()
                    .filter(item -> item.getId().equals(reponse.questionId()))
                    .findFirst()
                    .orElseThrow(() -> new StageBusinessException(
                            HttpStatus.BAD_REQUEST,
                            "Question invalide pour ce QCM"
                    ));
            OptionReponse option = optionReponseRepository.findById(reponse.optionId())
                    .orElseThrow(() -> new StageBusinessException(
                            HttpStatus.BAD_REQUEST,
                            "Option de réponse invalide"
                    ));
            if (!optionReponseRepository.existsByIdAndQuestionId(
                    option.getId(),
                    question.getId()
            )) {
                throw new StageBusinessException(
                        HttpStatus.BAD_REQUEST,
                        "L'option choisie n'appartient pas à la question indiquée"
                );
            }
            reponses.put(question.getId(), option.getId());
        }
        return reponses;
    }

    private int calculerScore(List<Question> questions, Map<Long, Long> reponses) {
        int pointsObtenus = 0;
        int pointsTotal = 0;
        for (Question question : questions) {
            pointsTotal += question.getPoints();
            Long optionChoisieId = reponses.get(question.getId());
            OptionReponse optionChoisie = optionReponseRepository.findById(optionChoisieId)
                    .orElseThrow();
            if (optionChoisie.isEstCorrecte()) {
                pointsObtenus += question.getPoints();
            }
        }
        if (pointsTotal == 0) {
            return 0;
        }
        return Math.round((pointsObtenus * 100f) / pointsTotal);
    }

    private void verifierPeutTenter(Utilisateur etudiant, Qcm qcm) {
        if (tentativeQcmRepository.existsByEtudiantIdAndQcmIdAndReussiTrue(
                etudiant.getId(),
                qcm.getId()
        )) {
            throw new StageBusinessException(
                    HttpStatus.CONFLICT,
                    "Vous avez déjà réussi ce QCM d'admissibilité"
            );
        }
        long tentatives = tentativeQcmRepository.countByEtudiantIdAndQcmId(
                etudiant.getId(),
                qcm.getId()
        );
        if (tentatives >= qcm.getNombreMaxTentatives()) {
            throw new StageBusinessException(
                    HttpStatus.CONFLICT,
                    "Vous avez épuisé toutes vos tentatives pour ce QCM"
            );
        }
    }

    private Qcm qcmActif() {
        return qcmRepository.findFirstByActifTrueOrderByIdAsc()
                .orElseThrow(() -> new StageBusinessException(
                        HttpStatus.NOT_FOUND,
                        "Aucun QCM d'admissibilité n'est disponible pour le moment"
                ));
    }

    private String messageResultat(boolean reussi, QcmStatutResponse statut) {
        if (reussi) {
            return "Félicitations ! Vous pouvez maintenant postuler aux offres de stage.";
        }
        if (statut.tentativesRestantes() > 0) {
            return "Score insuffisant. Il vous reste "
                    + statut.tentativesRestantes()
                    + " tentative(s).";
        }
        return "Score insuffisant et plus aucune tentative disponible.";
    }

    private Utilisateur etudiant(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() != Role.ETUDIANT) {
            throw new StageBusinessException(HttpStatus.FORBIDDEN, "Accès réservé aux étudiants");
        }
        return utilisateur;
    }
}
